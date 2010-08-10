/*
	Milyn - Copyright (C) 2006 - 2010

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License (version 2.1) as published by the Free Software
	Foundation.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

	See the GNU Lesser General Public License for more details:
	http://www.gnu.org/licenses/lgpl.txt
*/
package org.milyn.ect.formats.unedifact;

import org.milyn.ect.EdiParseException;
import org.milyn.edisax.model.internal.*;
import org.milyn.ect.common.XmlTagEncoder;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UnEdifactMessage
 * @author bardl
 */
public class UnEdifactMessage {

    /**
     * Marks the start of the Message Definition section.
     */
    private static final String MESSAGE_DEFINITION = "[\\d\\. ]*MESSAGE DEFINITION";

    /**
     * Extracts description from start of segment documentation.
     * Group1 = id
     * Group2 = documentation
     */
    private static final String MESSAGE_DEFINITION_START = "^(\\d{4}) *(.*)";

    /**
     * Marks the end of the Message Definition section.
     */
    private static final String MESSAGE_DEFINITION_END = "([\\d\\.]* *(Data)? *[S|s]egment index .*)";


    /**
     * Extracts the value for Message type, version, release and agency.
     */
    private static final Pattern MESSAGE_TYPE = Pattern.compile(".*Message Type *: *(\\w*)");
    private static final Pattern MESSAGE_RELEASE = Pattern.compile(".*Release *: *(\\w*)");
    private static final Pattern MESSAGE_AGENCY = Pattern.compile(".*Contr. Agency *: *(\\w*)");
    private static final Pattern MESSAGE_VERSION = Pattern.compile(".*Version *: *(\\w*)");

    /**
     * Marks the start of the Segment table section.
     */
    private static final String SEGMENT_TABLE = "[\\d\\. ]*Segment table";
    private static final String SEGMENT_TABLE_HEADER = "Pos *Tag *Name *S *R.*";

    /**
     * Extracts information from Regular segment definition.
     * Group1 = id
     * Group2 = segcode
     * Group3 = description
     * Group4 = isMandatory
     * Group5 = max occurance
     */
    private static String SEGMENT_REGULAR = "(\\d{4})[\\+\\* ]*(\\w{3}) *(.*) *(M|C) *(\\d*)[ \\|]*";

    /**
     * Matches and extracts information from start of segment group.
     * Group1 = id
     * Group2 = name
     * Group4 = isMandatory
     * Group5 = max occurance 
     */
    private static String SEGMENT_GROUP_START = "(\\d{4})[\\+\\* ]*-* *(Segment group \\d*) *-* *(C|M) *(\\d*)[-+|]*";

    /**
     * Matches and extracts information from segment at end of segment group.
     * Group1 = id
     * Group2 = segcode
     * Group3 = description
     * Group4 = isMandatory
     * Group5 = max occurance
     * Group6 = nrOfClosedGroups
     */
    private static String SEGMENT_GROUP_END = "(\\d{4})[\\+\\* ]*(\\w{3}) *([\\w /]*) *(C|M) *(\\d*)([-|\\+]*)";

    /**
     * Newline character applied between documentation lines.
     */
    private static final String NEW_LINE = "\n";

    /**
     * A message must match the LEGAL_MESSAGE pattern. Otherwise it may be an index file located in the message folder.
     */
    private static final String LEGAL_MESSAGE = " *UN/EDIFACT";

    /**
     * Default settings for UN/EDIFACT.
     */
    private static final String DELIMITER_SEGMENT = "&#39;!$";
    private static final String DELIMITER_COMPOSITE = "+";
    private static final String DELIMITER_DATA = ":";
    private static final String DELIMITER_NOT_USED = "~";
    private static final String ESCAPE = "?";

    private static List<String> ignoreSegments = Arrays.asList("UNA", "UNB", "UNG", "UNH", "UNT", "UNZ", "UNE");

    private String type;
    private String version;
    private String release;
    private String agency;
    private Edimap edimap;

    public UnEdifactMessage(Reader reader, boolean isSplitIntoImport, Edimap definitionModel) throws EdiParseException, IOException {

        BufferedReader breader = null;
        try {

            breader = new BufferedReader(reader);

            assertLegalMessage(breader);

            type = getValue(breader, MESSAGE_TYPE);
            version = getValue(breader, MESSAGE_VERSION);
            release = getValue(breader, MESSAGE_RELEASE);
            agency = getValue(breader, MESSAGE_AGENCY);

            edimap = new Edimap();
            SegmentGroup rootGroup = new SegmentGroup();
            rootGroup.setXmltag(XmlTagEncoder.encode(type));
            edimap.setSegments(rootGroup);            

            Delimiters delimiters = new Delimiters();
            delimiters.setSegment(DELIMITER_SEGMENT);
            delimiters.setField(DELIMITER_COMPOSITE);
            delimiters.setComponent(DELIMITER_DATA);
            delimiters.setSubComponent(DELIMITER_NOT_USED);
            delimiters.setEscape(ESCAPE);
            edimap.setDelimiters(delimiters);

            edimap.setDescription(new Description());
            edimap.getDescription().setName(type);
            edimap.getDescription().setVersion(version + ":" + release + ":" + agency);

            Map<String, Segment> segmentDefinitions = null;
            if (isSplitIntoImport) {
                Import ediImport = new Import();
                ediImport.setNamespace(agency);
                ediImport.setResource(definitionModel.getDescription().getName() + ".xml");  // TODO: Review with B�rd
                edimap.getImports().add(ediImport);
            }  else {
                segmentDefinitions = getSegmentDefinitions(definitionModel);
            }


            Map<String, String> definitions = parseMessageDefinition(breader);


            parseMessageStructure(breader, rootGroup, definitions, isSplitIntoImport, segmentDefinitions);

        } finally {
            if (breader != null) {
                breader.close();
            }
        }
    }

    public Edimap getEdimap() {
        return edimap;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getRelease() {
        return release;
    }

    public String getAgency() {
        return agency;
    }

    private Map<String, Segment> getSegmentDefinitions(Edimap definitionModel) {
        Map<String, Segment> result = new HashMap<String, Segment>();
        for (SegmentGroup segmentGroup : definitionModel.getSegments().getSegments()) {
            result.put(segmentGroup.getSegcode(), (Segment)segmentGroup);
        }
        return result;
    }

    private static void assertLegalMessage(BufferedReader reader) throws EdiParseException {
        String line;

        try {
            line = reader.readLine();
        } catch (IOException e) {
            throw new EdiParseException("Error reading first line of UN/EDIFACT message.", e);
        }

        if(!line.matches(LEGAL_MESSAGE)) {
            throw new EdiParseException("Not a valid UN/EDIFACT message definition.  First line doe not match pattern '" + LEGAL_MESSAGE + "'.");
        }
    }

    private void parseMessageStructure(BufferedReader reader, SegmentGroup group, Map<String, String> definitions, boolean isSplitIntoImport, Map<String, Segment> segmentDefinitions) throws IOException {
        String line = reader.readLine();
        while (!line.matches(SEGMENT_TABLE)) {
            line = reader.readLine();
        }

        while (!line.matches(SEGMENT_TABLE_HEADER)) {
            line = reader.readLine();
        }
        parseNextSegment(reader, group, definitions, isSplitIntoImport, segmentDefinitions);
    }

    private Map<String, String> parseMessageDefinition(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        while (!line.matches(MESSAGE_DEFINITION)) {
            line = reader.readLine();
        }

        while (!line.matches(MESSAGE_DEFINITION_START)) {
            line = reader.readLine();
        }

        Map<String, String> definitions = new HashMap<String, String>();
        while (!line.matches(MESSAGE_DEFINITION_END)) {
            if (line.matches(MESSAGE_DEFINITION_START)) {
                Pattern pattern = Pattern.compile(MESSAGE_DEFINITION_START);
                Matcher matcher = pattern.matcher(line);
                matcher.matches();

                String id = matcher.group(1);
                StringBuilder definition = new StringBuilder();
                definition.append(matcher.group(2)).append(NEW_LINE);
                line = reader.readLine();

                while (!line.matches(MESSAGE_DEFINITION_START) && !line.matches(MESSAGE_DEFINITION_END)) {
                    definition.append(line).append(NEW_LINE);
                    line = reader.readLine();
                }
                definitions.put(id, definition.toString());
            } else {
                line = reader.readLine();
            }

        }
        return definitions;
    }

    private int parseNextSegment(BufferedReader reader, SegmentGroup parentGroup, Map<String, String> definitions, boolean isSplitIntoImport, Map<String, Segment> segmentDefinitions) throws IOException {
        String line = reader.readLine();
        while (line != null) {
            if (line.matches(SEGMENT_GROUP_START)) {
                Matcher matcher = Pattern.compile(SEGMENT_GROUP_START).matcher(line);
                matcher.matches();
                SegmentGroup group = createGroup(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), definitions);
                parentGroup.getSegments().add(group);

                int result = parseNextSegment(reader, group, definitions, isSplitIntoImport, segmentDefinitions);
                if (result != 0) {
                    return result - 1;
                }

            } else if (line.matches(SEGMENT_GROUP_END)) {
                Matcher matcher = Pattern.compile(SEGMENT_GROUP_END).matcher(line);
                matcher.matches();
                Segment segment = createSegment(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), matcher.group(5), definitions, isSplitIntoImport, segmentDefinitions);
                parentGroup.getSegments().add(segment);
                return extractPlusCharacter(matcher.group(6)).length() - 1;
            } else if (line.matches(SEGMENT_REGULAR)) {
                Matcher matcher = Pattern.compile(SEGMENT_REGULAR).matcher(line);
                matcher.matches();
                if (!ignoreSegments.contains(matcher.group(2))) {
                    Segment segment = createSegment(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), matcher.group(5), definitions, isSplitIntoImport, segmentDefinitions);
                    parentGroup.getSegments().add(segment);
                }
            }
            
            line = reader.readLine();
        }
        return 0;
    }

    private String extractPlusCharacter(String value) {
        return value.replaceAll("[^\\+]", "");
    }

    private SegmentGroup createGroup(String id, String name, String mandatory, String maxOccurance, Map<String, String> definitions) {
        SegmentGroup group = new SegmentGroup();
        group.setXmltag(XmlTagEncoder.encode(name.trim()));
        group.setDocumentation(definitions.get(id).trim());
        group.setMinOccurs(mandatory.equals("M") ? 1 : 0);
        group.setMaxOccurs(Integer.valueOf(maxOccurance));
        return group;
    }

    private Segment createSegment(String id, String segcode, String description, String mandatory, String maxOccurance, Map<String, String> definitions, boolean isSplitIntoImport, Map<String, Segment> segmentDefinitions) {
        Segment segment = new Segment();

        segment.setSegcode(segcode);
        segment.setNodeTypeRef(agency + ":" + segcode);

        if (!isSplitIntoImport) {
            Segment importedSegment = segmentDefinitions.get(segcode);

            if(importedSegment == null) {
                throw new EdiParseException("Unknown segment code '" + segcode + "'.");
            }

            segment.getFields().addAll(importedSegment.getFields());

            if (importedSegment.getSegments().size() > 0) {
                segment.getSegments().addAll(importedSegment.getSegments());
            }
        }
        segment.setXmltag(XmlTagEncoder.encode(description.trim()));
        segment.setDocumentation(definitions.get(id).trim());
        segment.setMinOccurs(mandatory.equals("M") ? 1 : 0);
        segment.setMaxOccurs(Integer.valueOf(maxOccurance));
        segment.setTruncatable(true);
        return segment;
    }

    private String getValue(BufferedReader reader, Pattern pattern) throws IOException {
        String line = reader.readLine();
        Matcher matcher = pattern.matcher(line);
        while (!matcher.matches()) {
            line = reader.readLine();
            matcher = pattern.matcher(line);
        }
        return matcher.group(1);
    }
}