package org.cloud.common;

public class Commands {
    public static final String DELIMITER = "§";
    public static final String SEND_FILE = "/send_file";
    public static final String RETRIEVE_FILE = "/retrieve_file";
    public static final String DIR_STRUCTURE = "/dir_structure";

    public static String getDirStructure(String content) {
        return DIR_STRUCTURE + DELIMITER + content;
    }
    public static String getSendFile(String fileName) {
        return SEND_FILE + DELIMITER + fileName;
    }
    public static String getRetrieveFile (String fileName) {
        return RETRIEVE_FILE + DELIMITER + fileName;
    }




}
