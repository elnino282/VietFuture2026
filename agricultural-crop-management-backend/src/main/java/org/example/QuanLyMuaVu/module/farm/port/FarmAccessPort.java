package org.example.QuanLyMuaVu.module.farm.port;

import java.util.List;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;

public interface FarmAccessPort {

    Long getCurrentUserId();

    org.example.QuanLyMuaVu.module.identity.entity.User getCurrentUser();

    List<Integer> getAccessibleFarmIdsForCurrentUser();

    void assertCurrentUserCanAccessFarm(Farm farm);

    void assertCurrentUserCanAccessPlot(Plot plot);

    void assertCurrentUserCanAccessSeason(org.example.QuanLyMuaVu.module.season.entity.Season season);

    void assertCurrentUserCanAccessWarehouse(org.example.QuanLyMuaVu.module.inventory.entity.Warehouse warehouse);
}
