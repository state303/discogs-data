package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import io.dsub.discogsdata.batch.exception.UnknownDumpTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SimpleDumpFetcher implements DumpFetcher {

    private static final String DISCOGS_DATA_BASE_URL = "https://data.discogs.com/";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"));
    public static String LAST_KNOWN_BUCKET_URL = "https://discogs-data.s3-us-west-2.amazonaws.com";

    private static boolean isWithinValidRange(LocalDateTime localDateTime) {
        LocalDate that = LocalDate.of(2018, 1, 1);
        LocalDate localDate = localDateTime.toLocalDate();
        return (localDate.isEqual(that) || localDate.isAfter(that));
    }

    private static DumpType getDumpType(String dumpKey) {
        if (dumpKey.contains("releases")) return DumpType.RELEASE;
        if (dumpKey.contains("artists")) return DumpType.ARTIST;
        if (dumpKey.contains("labels")) return DumpType.LABEL;
        if (dumpKey.contains("masters")) return DumpType.MASTER;
        throw new UnknownDumpTypeException("failed to parse dump type from " + dumpKey);
    }

    private static void updateLastKnownBucketUrl(String urlString) {
        URL url;

        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            log.warn("Malformed url detected. Leaving known bucket url as is.");
            return;
        }

        try (InputStream in = url.openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

            String target = reader.lines()
                    .filter(s -> s.contains("BUCKET_URL"))
                    .collect(Collectors.joining());

            target = Arrays.stream(target.split("'"))
                    .filter(s -> s.contains("discogs"))
                    .collect(Collectors.joining());

            LAST_KNOWN_BUCKET_URL = "https:" + target;
        } catch (IOException e) {
            log.warn("Exception: " + e.getMessage() + ". Leaving known bucket url as is.");
        }
    }

    public List<DiscogsDump> getDiscogsDumps() {
        return getDiscogsDumps(LAST_KNOWN_BUCKET_URL);
    }

    public List<DiscogsDump> getDiscogsDumps(String discogsS3BucketUrl) {
        List<DiscogsDump> dumpList = new ArrayList<>();
        URL url;

        try {
            url = new URL(discogsS3BucketUrl);
        } catch (MalformedURLException e) {
            log.error("Malformed url detected. Returning empty container...");
            return new ArrayList<>();
        }

        try (InputStream in = url.openStream()) {

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newDefaultInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();

            Document document = builder.parse(in);
            NodeList contents = document.getElementsByTagName("Contents");

            for (int i = 0; i < contents.getLength(); i++) {
                Node contentNode = contents.item(i);
                NodeList dataNodeList = contentNode.getChildNodes();

                if (dataNodeList.item(0).getTextContent().matches("\\S+.xml.gz")) {
                    DiscogsDump dump = parseDump(dataNodeList);
                    if (dump != null &&
                            dump.getLastModified() != null &&
                            isWithinValidRange(dump.getLastModified())) {
                        dumpList.add(dump);
                    }
                }
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
            log.error(e.getClass().getName() + ": " + e.getMessage());
        } catch (NumberFormatException e) {
            log.error("Dump item size has invalid data. Skipping..");
        }
        return dumpList;
    }

    public DiscogsDump parseDump(NodeList dataNodeList) {
        DumpType dumpType = null;
        String uri = "";
        LocalDateTime lastModified = null;
        String etag = "";
        long size = 0L;

        try {
            for (int j = 0; j < dataNodeList.getLength(); j++) {
                Node data = dataNodeList.item(j);

                switch (data.getNodeName()) {
                    case "Key":
                        uri = data.getTextContent();
                        dumpType = getDumpType(uri);
                        break;
                    case "LastModified":
                        lastModified = LocalDateTime.parse(data.getTextContent(), FORMATTER);
                        break;
                    case "ETag":
                        etag = data.getTextContent().replace("\"", "");
                        break;
                    case "Size":
                        size = Long.parseLong(data.getTextContent());
                        break;
                }
            }
        } catch (NumberFormatException e) {
            log.error("dump item " + uri + " has invalid data. skipping item...");
            return null;
        } catch (UnknownDumpTypeException e) {
            log.error(e.getMessage() + ". skipping item...");
            return null;
        } catch (DateTimeParseException e) {
            log.error("dump item " + uri + " has invalid LastModified entry. skipping item...");
        }

        return DiscogsDump.builder()
                .uri(uri)
                .dumpType(dumpType)
                .etag(etag)
                .size(size)
                .lastModified(lastModified)
                .build();
    }

    public String getS3BucketUrl() {
        updateLastKnownBucketUrl(DISCOGS_DATA_BASE_URL);
        return LAST_KNOWN_BUCKET_URL;
    }
}
