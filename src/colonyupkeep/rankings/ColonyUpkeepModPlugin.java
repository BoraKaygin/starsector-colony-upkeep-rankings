package colonyupkeep.rankings;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;

import java.util.List;

public final class ColonyUpkeepModPlugin extends BaseModPlugin {

    @Override
    public void onGameLoad(boolean newGame) {
        IntelManagerAPI intelManager = Global.getSector().getIntelManager();
        List<IntelInfoPlugin> existing = intelManager.getIntel(ColonyUpkeepIntel.class);

        for (IntelInfoPlugin intel : existing) {
            if (intel instanceof ColonyUpkeepIntel && !intel.shouldRemoveIntel()) {
                ColonyUpkeepIntel upkeepIntel = (ColonyUpkeepIntel) intel;
                upkeepIntel.refreshNow();
                return;
            }
        }

        ColonyUpkeepIntel intel = new ColonyUpkeepIntel();
        intel.refreshNow();
        intel.setImportant(true);
        intelManager.addIntel(intel, false);
    }
}
