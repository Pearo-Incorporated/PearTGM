package com.minehut.tgm.gametype;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by luke on 4/27/17.
 */
@AllArgsConstructor
public enum GameType {
    TDM("TDM", TDMManifest.class),
    KOTH("King of the Hill", KOTHManifest.class);

    @Getter private String name;
    @Getter private Class manifest;
}
