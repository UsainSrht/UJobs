package me.usainsrht.ujobs.yaml;

import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
@Builder
public class YamlCommand { //doesnt implement yaml object because Command object is not parseable.

    @NotNull private String name;
    private String description;
    private String usage;
    private List<String> aliases;
    @Nullable private String permission;
    @Nullable private YamlMessage permissionMessage;

}