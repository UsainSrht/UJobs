package me.usainsrht.ujobs;

import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
public class PlayerData {

    UUID uuid;
    Map<Job, Double> exps;
    Map<Job, Integer> levels;

}
