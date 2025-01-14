package io.codiga.plugins.jetbrains.dependencies;

import io.codiga.plugins.jetbrains.model.Dependency;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.codiga.plugins.jetbrains.Constants.*;

public class RubyDependency extends AbstractDependency{


    @VisibleForTesting
    @Override
    public List<Dependency> getDependenciesFromInputStream(InputStream inputStream) {
        List<Dependency> result = new ArrayList<>();
        String pattern = "^\\s*gem\\s+[\\'|\\\"]([a-zA-Z0-9\\-]+)[\\'|\\\"]";
        Pattern r = Pattern.compile(pattern);
        try{
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null){
                System.out.println(line);
                // if there is a comment, do not use
                if (line.contains("#")) {
                    continue;
                }
                Matcher m = r.matcher(line);
                if (m.find()) {
                    result.add(new Dependency(m.group(1), Optional.empty()));
                }
            }

            return result;
        }
        catch (IOException e) {
            LOGGER.info("PythonDependency - getDependenciesFromInputStream - error when parsing the file");
            return ImmutableList.of();
        }
    }

    @Override
    public String getDependencyFilename() {
        return RUBY_DEPENDENCY_FILE;
    }
}
