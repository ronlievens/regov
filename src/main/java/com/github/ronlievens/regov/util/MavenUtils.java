package com.github.ronlievens.regov.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MavenUtils {

    public static Model parsePom(@NonNull final Path file) throws IOException, XmlPullParserException {
        val reader = new MavenXpp3Reader();
        return reader.read(new FileReader(file.toFile()));
    }

    public static Model parsePom(@NonNull final String fileContent) throws XmlPullParserException, IOException {
        val reader = new MavenXpp3Reader();
        return reader.read(new StringReader(fileContent));
    }

    public static String parsePom(@NonNull final Model model) throws IOException {
        val writer = new MavenXpp3Writer();
        val stringWriter = new StringWriter();
        writer.write(stringWriter, model);
        return stringWriter.toString();
    }
}
