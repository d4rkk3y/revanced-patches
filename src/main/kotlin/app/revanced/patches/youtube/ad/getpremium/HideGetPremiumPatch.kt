package app.revanced.patches.youtube.ad.getpremium

import app.revanced.util.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.settings.preference.impl.StringResource
import app.revanced.patches.shared.settings.preference.impl.SwitchPreference
import app.revanced.patches.youtube.ad.getpremium.fingerprints.GetPremiumViewFingerprint
import app.revanced.patches.youtube.misc.integrations.IntegrationsPatch
import app.revanced.patches.youtube.misc.settings.SettingsPatch
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch(
    description = "Hides YouTube Premium signup promotions under the video player.",
    dependencies = [IntegrationsPatch::class, SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube", [
                "18.32.39",
                "18.37.36",
                "18.38.44",
                "18.43.45",
                "18.44.41",
                "18.45.41",
                "18.45.43"
            ]
        )
    ]
)
object HideGetPremiumPatch : BytecodePatch(setOf(GetPremiumViewFingerprint)) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "Lapp/revanced/integrations/patches/HideGetPremiumPatch;"

    override fun execute(context: BytecodeContext) {
        SettingsPatch.PreferenceScreen.ADS.addPreferences(
            SwitchPreference(
                "revanced_hide_get_premium",
                StringResource(
                    "revanced_hide_get_premium_title",
                    "Hide YouTube Premium promotions"
                ),
                StringResource(
                    "revanced_hide_get_premium_summary_on",
                    "YouTube Premium promotions under video player is hidden"
                ),
                StringResource(
                    "revanced_hide_get_premium_summary_off",
                    "YouTube Premium promotions under video player is shown"
                )
            )
        )

        GetPremiumViewFingerprint.result?.let {
            it.mutableMethod.apply {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val measuredWidthRegister = getInstruction<TwoRegisterInstruction>(startIndex).registerA
                val measuredHeightInstruction = getInstruction<TwoRegisterInstruction>(startIndex + 1)

                val measuredHeightRegister = measuredHeightInstruction.registerA
                val tempRegister = measuredHeightInstruction.registerB

                addInstructionsWithLabels(
                    startIndex + 2,
                    """
                        # Override the internal measurement of the layout with zero values.
                        invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->hideGetPremiumView()Z
                        move-result v$tempRegister
                        if-eqz v$tempRegister, :allow
                        const/4 v$measuredWidthRegister, 0x0
                        const/4 v$measuredHeightRegister, 0x0
                        :allow
                        nop
                        # Layout width/height is then passed to a protected class method.
                    """
                )
            }
        } ?: throw GetPremiumViewFingerprint.exception
    }
}
