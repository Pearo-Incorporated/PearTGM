package network.warzone.tgm.modules.koth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import network.warzone.tgm.TGM;
import network.warzone.tgm.match.Match;
import network.warzone.tgm.match.MatchModule;
import network.warzone.tgm.modules.controlpoint.ControlPoint;
import network.warzone.tgm.modules.controlpoint.ControlPointDefinition;
import network.warzone.tgm.modules.points.PointsModule;
import network.warzone.tgm.modules.region.Region;
import network.warzone.tgm.modules.region.RegionManagerModule;
import network.warzone.tgm.modules.scoreboard.ScoreboardInitEvent;
import network.warzone.tgm.modules.scoreboard.ScoreboardManagerModule;
import network.warzone.tgm.modules.scoreboard.SimpleScoreboard;
import network.warzone.tgm.modules.team.MatchTeam;
import network.warzone.tgm.modules.team.TeamManagerModule;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class KOTHModule extends MatchModule implements Listener {
    @Getter private final List<ControlPoint> controlPoints = new ArrayList<>();
    @Getter private PointsModule pointsModule;

    @Getter
    private final HashMap<ControlPointDefinition, Integer> controlPointScoreboardLines = new HashMap<>();

    @Getter
    private final HashMap<MatchTeam, Integer> teamScoreboardLines = new HashMap<>();

    @Override
    public void load(Match match) {
        JsonObject kothJson = match.getMapContainer().getMapInfo().getJsonObject().get("koth").getAsJsonObject();

        for (JsonElement capturePointElement : kothJson.getAsJsonArray("hills")) {
            JsonObject capturePointJson = capturePointElement.getAsJsonObject();
            Region region = match.getModule(RegionManagerModule.class).getRegion(match, capturePointJson.get("region"));
            int timeToCap = 10;
            if (capturePointJson.has("time")) {
                timeToCap = capturePointJson.get("time").getAsInt();
            }
            int swap = 1;
            if (capturePointJson.has("points")) {
                swap = capturePointJson.get("points").getAsInt();
            }
            final int pointsPerHold = swap;
            final String name = capturePointJson.get("name").getAsString();

            ControlPointDefinition definition = new ControlPointDefinition(name, timeToCap, pointsPerHold);
            ControlPoint controlPoint = new ControlPoint(definition, region, new KOTHControlPointService(this, match, definition));

            controlPoints.add(controlPoint);
        }

        for (ControlPoint controlPoint : controlPoints) {
            controlPoint.enable();
        }

        pointsModule = match.getModule(PointsModule.class);
        pointsModule.addService(matchTeam -> TGM.get().getMatchManager().endMatch(matchTeam));
    }


    public void incrementPoints(MatchTeam matchTeam, int amount) {
        pointsModule.incrementPoints(matchTeam, amount);
        updateScoreboardTeamLine(matchTeam);
    }

    public void updateScoreboardTeamLine(MatchTeam matchTeam) {
        for (SimpleScoreboard simpleScoreboard : TGM.get().getModule(ScoreboardManagerModule.class).getScoreboards().values()) {
            int line = teamScoreboardLines.get(matchTeam);
            simpleScoreboard.remove(line);
            simpleScoreboard.add(getTeamScoreLine(matchTeam), line);
            simpleScoreboard.update();
        }
    }

    public void updateScoreboardControlPointLine(ControlPointDefinition definition) {
        for (SimpleScoreboard simpleScoreboard : TGM.get().getModule(ScoreboardManagerModule.class).getScoreboards().values()) {
            int line = controlPointScoreboardLines.get(definition);
            simpleScoreboard.remove(line);
            simpleScoreboard.add(getControlPointScoreboardLine(getControlPointByDefinition(definition)), line);
            simpleScoreboard.update();
        }
    }

    public ControlPoint getControlPointByDefinition(ControlPointDefinition definition) {
        for (ControlPoint controlPoint : controlPoints) {
            if(controlPoint.getDefinition() == definition) return controlPoint;
        }
        return null;
    }

    @EventHandler
    public void onScoreboardInit(ScoreboardInitEvent event) {
        List<MatchTeam> teams = TGM.get().getModule(TeamManagerModule.class).getTeams();


        SimpleScoreboard simpleScoreboard = event.getSimpleScoreboard();

        int i;
        for(i = 0; i < controlPoints.size(); i++) {
            ControlPoint controlPoint = controlPoints.get(i);

            controlPointScoreboardLines.put(controlPoint.getDefinition(), i);
            simpleScoreboard.add(getControlPointScoreboardLine(controlPoint), i);
        }

        i++;
        simpleScoreboard.add(" ", i);

        i++;
        for (MatchTeam matchTeam : teams) {
            if(matchTeam.isSpectator()) continue;

            simpleScoreboard.add(getTeamScoreLine(matchTeam), i);
            teamScoreboardLines.put(matchTeam, i);

            i++;
        }
    }

    private String getTeamScoreLine(MatchTeam matchTeam) {
        return pointsModule.getPoints(matchTeam) + ChatColor.DARK_GRAY.toString() + "/" + ChatColor.GRAY.toString() + pointsModule.getTarget(matchTeam) + " " + matchTeam.getColor() + matchTeam.getAlias();
    }

    private String getControlPointScoreboardLine(ControlPoint controlPoint) {
        if (controlPoint.isInProgress()) {
            if (controlPoint.getController() == null) {
                return controlPoint.getProgressingTowardsTeam().getColor().toString() + controlPoint.getPercent() + "% " + ChatColor.WHITE + controlPoint.getDefinition().getName();
            } else {
                return controlPoint.getPercent() + "% " + controlPoint.getController().getColor() + controlPoint.getDefinition().getName();
            }
        } else {
            if (controlPoint.getController() == null) {
                return ControlPoint.SYMBOL_CP_INCOMPLETE + " " + controlPoint.getDefinition().getName();
            } else {
                return ControlPoint.SYMBOL_CP_COMPLETE + " " + controlPoint.getController().getColor() + controlPoint.getDefinition().getName();
            }
        }
    }

    @Override
    public void disable() {
        for (ControlPoint controlPoint : controlPoints) {
            controlPoint.unload();
        }
    }

    @Override
    public void unload() {
        for (ControlPoint controlPoint : controlPoints) {
            controlPoint.unload();
        }
    }
}
