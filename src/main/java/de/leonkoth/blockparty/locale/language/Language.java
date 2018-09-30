package de.leonkoth.blockparty.locale.language;

import de.leonkoth.blockparty.locale.Locale;
import de.leonkoth.blockparty.locale.LocaleSection;
import de.leonkoth.blockparty.locale.LocaleString;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;

public abstract class Language {

    protected static void writeTo(Class<?> languageClass, File file, boolean overWrite) throws IOException {

        if (!file.exists() || overWrite) {
            file.getParentFile().mkdirs();
            file.createNewFile();

            FileConfiguration configuration = new YamlConfiguration();
            for (Field field : languageClass.getFields()) {
                if (field.getType() == LocaleSection.class) {

                    try {
                        LocaleSection localeSection = (LocaleSection) field.get(null);
                        configuration.set("Sections." + localeSection, localeSection.getPrefixColor().name());
                    } catch(IllegalAccessException e) {
                        e.printStackTrace();
                    }

                } else if (field.getType() == LocaleString.class) {

                    try {
                        LocaleString localeString = (LocaleString) field.get(null);
                        String[] values = localeString.getValues();
                        String path = localeString.getSection() + "." + Locale.convertName(field.getName());

                        if (values.length == 1) {
                            configuration.set(path, values[0]);
                        } else {
                            configuration.set(path, Arrays.asList(values));
                        }

                    } catch(IllegalAccessException e) {
                        e.printStackTrace();
                    }

                }

            }

            configuration.save(file);
        }
    }

}
