public final class VerifyLoad {
    public static void main(String[] args) throws Exception {
        Class<?> pluginClass = Class.forName("colonyupkeep.rankings.ColonyUpkeepModPlugin");
        Class<?> intelClass = Class.forName("colonyupkeep.rankings.ColonyUpkeepIntel");
        Class<?> basePluginClass = Class.forName("com.fs.starfarer.api.BaseModPlugin");
        Class<?> intelInfoClass = Class.forName("com.fs.starfarer.api.campaign.comm.IntelInfoPlugin");
        Class<?> tooltipClass = Class.forName("com.fs.starfarer.api.ui.TooltipMakerAPI");

        Object plugin = pluginClass.getDeclaredConstructor().newInstance();
        Object intel = intelClass.getDeclaredConstructor().newInstance();

        if (!basePluginClass.isInstance(plugin)) {
            throw new AssertionError("Mod plugin does not extend BaseModPlugin");
        }
        if (!intelInfoClass.isInstance(intel)) {
            throw new AssertionError("Intel entry does not implement IntelInfoPlugin");
        }
        intelClass.getMethod("createSmallDescription", tooltipClass, float.class, float.class);
        if ((Boolean) intelClass.getMethod("hasSmallDescription").invoke(intel)) {
            throw new AssertionError("Small right-panel mode should be disabled");
        }
        if (!((Boolean) intelClass.getMethod("hasLargeDescription").invoke(intel))) {
            throw new AssertionError("Full-width Major Event mode should be enabled");
        }
        java.lang.reflect.Method formatVerifier = intelClass.getDeclaredMethod("verifyUiFormatStringsForTests");
        formatVerifier.setAccessible(true);
        formatVerifier.invoke(null);

        System.out.println("Verified full-width Major Event mode and formatted UI strings.");
    }
}
