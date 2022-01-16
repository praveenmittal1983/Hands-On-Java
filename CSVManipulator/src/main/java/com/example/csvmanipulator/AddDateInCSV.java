/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.csvmanipulator;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.FileReader;
import java.io.FileWriter;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import java.io.File;
import static java.lang.System.exit;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

import org.apache.log4j.Logger;

/**
 *
 * @author pmittal
 * This utility for conversion of XLSX file into CSV with required manipulation. Following are the high level steps:
 * 1. Checks filename format. It should be in 'string.string.string.number.number.xlsx'
 * 2. Extract date from filename.
 * 3. Verifying list of columns. It should be match the expected list of columns.
 * 4. Add new column name.
 * 5. Add Date in format (MM/DD/YYYY) to every row.
 * 6. Save it in CSV format with required filename.
 */
public class AddDateInCSV 
{
    //Setting up required variables
    private static final Logger LOG = Logger.getLogger(AddDateInCSV.class);
    private static final String FILENAME_PATTERN = "^\\w*_\\w*_\\w*.\\d*.(\\d{14}).csv";
    private static final String[] expectedColumns = {"ID","Name","Email","Phone","Country"};
    private static final boolean excludeHeader = false;
    private static String dateFromFile;
    private static final String newColumnName = "My Date";
    private static final String outputFilename = "MyOutputFile.csv";
                    
    public static void main(String[] args) 
    {
        
        //Application will not run without necessary arguments
        if (args.length == 0){
            LOG.error("Missing Arguments. Terminating");
            exit(1);
        }
        
        try 
        {
            //Get filename from user argument
            String fileName = args[0];
            LOG.info("Starting CSV Manipulator. Processing file " + fileName);
            
            //Validate filename & extract date
            String validationMsg = validateFilename_ExtractDate(fileName);
            if (!"Success".equals(validationMsg))
                throw new IOException(validationMsg);
            
            //Read input file
            List<String[]> rows = readFile(fileName, excludeHeader ? 1:0);
            List<String[]> modifiedRows = new ArrayList<>();
            List<String[]> rejectedRows = new ArrayList<>();
            boolean flagHeader = false;
            
            for (String[] row : rows) 
            {   
                //Validate header column and append new column name
                if (!flagHeader)
                {
                    if (validateHeader(row)){
                        String[] modifiedRow = addDateValue(row, newColumnName);
                        modifiedRows.add(modifiedRow);
                        flagHeader = true;
                        continue;
                    }
                    else {
                        throw new RuntimeException("Columns in file did not match expected list of columns");
                    } 
                }
                
                //Validate row and append date
                if (validateRow(row))
                {
                    String[] modifiedRow = addDateValue(row, dateFromFile);
                    modifiedRows.add(modifiedRow);                    
                }
                else
                {
                    rejectedRows.add(row);
                }
            }
            
            //Listing out rejected record details
            LOG.info("Number of records rejected: " + rejectedRows.size());
            for (String[] row : rejectedRows){
                LOG.info("Rejected Record: " + Arrays.toString(row));
            }
          
          //Writing data output file
          writeToFile(outputFilename, modifiedRows);
          LOG.info("File generated successfully " + outputFilename + ". Terminating application..");
        }
        catch (IOException ex) 
        {
            LOG.error("Failed to process: " + ex);
        }
    }

    private static List<String[]> readFile(String fileName, int excludeHeader) throws IOException 
    {
        CSVReader reader = new CSVReaderBuilder(new FileReader(fileName)).withSkipLines(excludeHeader).build();
        List<String[]> allRows = reader.readAll();
        LOG.info("Number of records read from input file: " + allRows.size());
        return allRows;
    }
    
    // Validate Filename & extract date from filename in required date format.
    private static String validateFilename_ExtractDate(String filename)
    {    
        File file = new File(filename);
        
        if (!file.exists())
        {
            return "File [" + filename + "] does not exist.";
        }
        
        if (file.isFile())
        {           
            Pattern pattern = Pattern.compile(FILENAME_PATTERN);
            Matcher m = pattern.matcher(file.getName());
            
            if (m.find()) {
                dateFromFile = formatDate(StringUtils.substring(m.group(1),0,8));
                if (dateFromFile != null) {
                    return "Success";
                }
            }
            
            return "Expected filename pattern is 'string_string_string.number.number.xlsx'.";
        }
        return "Failed: Reason: Unknown";
    }

    //Convert to required date format (DD/MM/YYYY)
    private static String formatDate(String inputString){
        SimpleDateFormat actualFormat = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat requiredFormat = new SimpleDateFormat("MM/dd/yyyy");
        
        try {
            return requiredFormat.format(actualFormat.parse(inputString));
        } catch (Exception ex) {
            LOG.error("Failed while converting date [" + inputString + "] to required format " + ex);
        }
        return null;
    }
    
    //Validating column list
    private static boolean validateHeader(String[] headerFromFile){
        int index=0;
        for (String item : headerFromFile){
            if (!item.trim().equals(expectedColumns[index])){
                return false;
            }
            index++;
        }
        return true;
    }
    
    //Validating row data
    private static boolean validateRow(String[] row){
        return row.length == expectedColumns.length && row[0].trim().length() > 0;
    }
    
    //Adding required date to row data
    private static String[] addDateValue(String[] row, String prefixData) 
    {   
        return (prefixData + "," + String.join(",", row)).split(",");
    }

    //Writing data to output file
    private static void writeToFile(String fileName, List<String[]> allRows) throws IOException 
    {
        try (CSVWriter writer = new CSVWriter(new FileWriter(fileName),',',CSVWriter.NO_QUOTE_CHARACTER)) 
        {
            //Writing Records
            for (String[] row : allRows) 
            {
                writer.writeNext(row);
            }
            LOG.info("Number of records written: " + allRows.size());
        }
    }
}
