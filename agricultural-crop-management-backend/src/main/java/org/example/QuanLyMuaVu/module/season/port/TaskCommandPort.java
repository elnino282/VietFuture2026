package org.example.QuanLyMuaVu.module.season.port;

import org.example.QuanLyMuaVu.module.season.entity.Task;

public interface TaskCommandPort {

    Task saveTask(Task task);
}
