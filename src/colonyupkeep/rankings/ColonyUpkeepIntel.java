package colonyupkeep.rankings;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ColonyUpkeepIntel extends BaseIntelPlugin {

    private static final float REFRESH_INTERVAL_SECONDS = 300f;
    private static final float BETA_UPKEEP_SAVING = 0.25f;
    private static final float OUTER_PAD = 10f;
    private static final long serialVersionUID = 1L;
    private static final String INTEL_COUNT_FORMAT = "%s structures ranked by current monthly upkeep.";
    private static final String INTEL_HIGHEST_FORMAT = "Highest: %s at %s (%s/month)";
    private static final String BETA_EXPLANATION_FORMAT =
            "Estimated Beta saving is %s of current upkeep when the structure does not already have an Alpha or Beta core. "
                    + "The list refreshes every %s and whenever the Intel screen is opened.";
    private static final String TOTALS_FORMAT =
            "Total listed upkeep: %s/month    Potential additional Beta savings: %s/month";

    private transient List<UpkeepRow> cachedRows;
    private transient float secondsSinceRefresh;

    public ColonyUpkeepIntel() {
        cachedRows = new ArrayList<UpkeepRow>();
    }

    @Override
    protected void advanceImpl(float amount) {
        secondsSinceRefresh += amount;
        if (secondsSinceRefresh >= REFRESH_INTERVAL_SECONDS) {
            refreshNow();
        }
    }

    @Override
    public void notifyPlayerAboutToOpenIntelScreen() {
        refreshNow();
    }

    public void refreshNow() {
        cachedRows = collectRows();
        secondsSinceRefresh = 0f;
    }

    private List<UpkeepRow> getRows() {
        if (cachedRows == null) {
            refreshNow();
        }
        return cachedRows;
    }

    private List<UpkeepRow> collectRows() {
        List<UpkeepRow> rows = new ArrayList<UpkeepRow>();

        if (Global.getSector() == null || Global.getSector().getEconomy() == null) {
            return rows;
        }

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market == null || !market.isPlayerOwned()) {
                continue;
            }

            for (Industry industry : market.getIndustries()) {
                if (industry == null || industry.isHidden() || industry.isBuilding()) {
                    continue;
                }

                float upkeep = industry.getUpkeep() == null
                        ? 0f
                        : Math.max(0f, industry.getUpkeep().getModifiedValue());
                String coreId = industry.getAICoreId();
                rows.add(new UpkeepRow(
                        safeIndustryName(industry),
                        safeMarketName(market),
                        upkeep,
                        getEstimatedBetaSaving(upkeep, coreId),
                        getCoreName(coreId),
                        hasUpkeepCore(coreId)
                ));
            }
        }

        Collections.sort(rows, new Comparator<UpkeepRow>() {
            @Override
            public int compare(UpkeepRow left, UpkeepRow right) {
                int upkeepOrder = Float.compare(right.upkeep, left.upkeep);
                if (upkeepOrder != 0) {
                    return upkeepOrder;
                }

                int colonyOrder = left.colonyName.compareToIgnoreCase(right.colonyName);
                if (colonyOrder != 0) {
                    return colonyOrder;
                }

                return left.industryName.compareToIgnoreCase(right.industryName);
            }
        });

        return rows;
    }

    private static String safeIndustryName(Industry industry) {
        String name = industry.getCurrentName();
        if (name == null || name.trim().isEmpty()) {
            name = industry.getId();
        }
        return name == null ? "Unknown structure" : name;
    }

    private static String safeMarketName(MarketAPI market) {
        String name = market.getName();
        if (name == null || name.trim().isEmpty()) {
            name = market.getId();
        }
        return name == null ? "Unknown colony" : name;
    }

    private static float getEstimatedBetaSaving(float upkeep, String coreId) {
        if (hasUpkeepCore(coreId)) {
            return 0f;
        }
        return upkeep * BETA_UPKEEP_SAVING;
    }

    private static boolean hasUpkeepCore(String coreId) {
        return Commodities.BETA_CORE.equals(coreId) || Commodities.ALPHA_CORE.equals(coreId);
    }

    private static String getCoreName(String coreId) {
        if (Commodities.ALPHA_CORE.equals(coreId)) {
            return "Alpha";
        }
        if (Commodities.BETA_CORE.equals(coreId)) {
            return "Beta";
        }
        if (Commodities.GAMMA_CORE.equals(coreId)) {
            return "Gamma";
        }
        if (coreId == null || coreId.trim().isEmpty()) {
            return "None";
        }
        return Misc.ucFirst(coreId.replace('_', ' '));
    }

    @Override
    public String getName() {
        return "Colony Upkeep Rankings";
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName("intel", "monthly_income_report");
    }

    @Override
    public FactionAPI getFactionForUIColors() {
        return Global.getSector().getFaction(Factions.PLAYER);
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = new LinkedHashSet<String>();
        tags.add(Tags.INTEL_MAJOR_EVENT);
        tags.add(Tags.INTEL_COLONIES);
        return tags;
    }

    @Override
    public boolean shouldRemoveIntel() {
        return false;
    }

    @Override
    public IntelInfoPlugin.IntelSortTier getSortTier() {
        return IntelInfoPlugin.IntelSortTier.TIER_0;
    }

    @Override
    public String getSortString() {
        return getName();
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, IntelInfoPlugin.ListInfoMode mode) {
        info.addTitle(getName());

        List<UpkeepRow> rows = getRows();
        if (rows.isEmpty()) {
            info.addPara("No operational structures on player-owned colonies.", 3f);
            return;
        }

        UpkeepRow top = rows.get(0);
        String upkeep = formatCredits(top.upkeep);
        info.addPara(
                INTEL_COUNT_FORMAT,
                3f,
                Misc.getHighlightColor(),
                Integer.toString(rows.size())
        );
        info.addPara(
                INTEL_HIGHEST_FORMAT,
                3f,
                Misc.getHighlightColor(),
                top.industryName,
                top.colonyName,
                upkeep
        );
    }

    @Override
    public boolean hasSmallDescription() {
        return false;
    }

    @Override
    public boolean hasLargeDescription() {
        return true;
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        refreshNow();
        addDescription(info, width);
    }

    @Override
    public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        refreshNow();

        TooltipMakerAPI info = panel.createUIElement(width, height, true);
        addDescription(info, width);
        panel.addUIElement(info).inTL(0f, 0f);
    }

    private void addDescription(TooltipMakerAPI info, float width) {
        Color highlight = Misc.getHighlightColor();
        Color text = Misc.getTextColor();
        Color gray = Misc.getGrayColor();
        Color positive = Misc.getPositiveHighlightColor();

        info.addTitle(getName());
        info.addPara(
                "All operational industries and structures on player-owned colonies, sorted by their current monthly upkeep. "
                        + "Values include live colony and industry modifiers.",
                OUTER_PAD
        );
        info.addPara(
                BETA_EXPLANATION_FORMAT,
                OUTER_PAD,
                highlight,
                "25%",
                "five active campaign minutes"
        );

        List<UpkeepRow> rows = getRows();
        if (rows.isEmpty()) {
            info.addSectionHeading("Structures", Alignment.MID, OUTER_PAD);
            info.addPara("No operational structures were found on player-owned colonies.", OUTER_PAD);
            return;
        }

        float totalUpkeep = 0f;
        float totalPotentialSaving = 0f;
        for (UpkeepRow row : rows) {
            totalUpkeep += row.upkeep;
            totalPotentialSaving += row.betaSaving;
        }

        String totalUpkeepText = formatCredits(totalUpkeep);
        String totalSavingText = formatCredits(totalPotentialSaving);
        info.addPara(
                TOTALS_FORMAT,
                OUTER_PAD,
                new Color[]{highlight, positive},
                totalUpkeepText,
                totalSavingText
        );

        float rankWidth = 42f;
        float upkeepWidth = 125f;
        float savingWidth = 135f;
        float coreWidth = 75f;
        float flexibleWidth = Math.max(260f, width - rankWidth - upkeepWidth - savingWidth - coreWidth - 20f);
        float structureWidth = flexibleWidth * 0.54f;
        float colonyWidth = flexibleWidth - structureWidth;

        FactionAPI player = getFactionForUIColors();
        info.beginTable(
                player,
                22f,
                "#", rankWidth,
                "Structure", structureWidth,
                "Colony", colonyWidth,
                "Upkeep / month", upkeepWidth,
                "Est. Beta saving", savingWidth,
                "Core", coreWidth
        );

        int rank = 1;
        for (UpkeepRow row : rows) {
            String savingText = row.hasUpkeepCore ? "Already applied" : formatCredits(row.betaSaving);
            Color savingColor = row.hasUpkeepCore ? gray : positive;

            info.addRow(
                    Alignment.RMID, gray, Integer.toString(rank),
                    Alignment.LMID, text, row.industryName,
                    Alignment.LMID, text, row.colonyName,
                    Alignment.RMID, highlight, formatCredits(row.upkeep),
                    Alignment.RMID, savingColor, savingText,
                    Alignment.LMID, getCoreColor(row.coreName), row.coreName
            );
            rank++;
        }

        info.addTable("No operational structures found.", 0, OUTER_PAD);
    }

    private static Color getCoreColor(String coreName) {
        if ("Alpha".equals(coreName)) {
            return new Color(255, 90, 90);
        }
        if ("Beta".equals(coreName)) {
            return new Color(255, 210, 70);
        }
        if ("Gamma".equals(coreName)) {
            return new Color(100, 190, 255);
        }
        return Misc.getGrayColor();
    }

    private static String formatCredits(float value) {
        return Misc.getDGSCredits(Math.round(value));
    }

    private static void verifyUiFormatStringsForTests() {
        String.format(INTEL_COUNT_FORMAT, "1");
        String.format(INTEL_HIGHEST_FORMAT, "Structure", "Colony", "1,000 credits");
        String.format(BETA_EXPLANATION_FORMAT, "25%", "five active campaign minutes");
        String.format(TOTALS_FORMAT, "10,000 credits", "2,500 credits");
    }

    private static final class UpkeepRow {
        private final String industryName;
        private final String colonyName;
        private final float upkeep;
        private final float betaSaving;
        private final String coreName;
        private final boolean hasUpkeepCore;

        private UpkeepRow(
                String industryName,
                String colonyName,
                float upkeep,
                float betaSaving,
                String coreName,
                boolean hasUpkeepCore
        ) {
            this.industryName = industryName;
            this.colonyName = colonyName;
            this.upkeep = upkeep;
            this.betaSaving = betaSaving;
            this.coreName = coreName;
            this.hasUpkeepCore = hasUpkeepCore;
        }
    }
}
