package de.geofabrik.railway_routing.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.LinkedList;

public class PatternMatching {
    
    public static int patternSplitDirFile(String pattern) {
        if (!pattern.contains("*") && !pattern.contains("?")) {
            return -1;
        }
        
        int firstStar = pattern.indexOf("*");
        int firstQuestionMark = pattern.indexOf("?");
        int firstWildcard = -1;
        if ((firstStar < firstQuestionMark && firstStar >= 0) || (firstStar > -1 && firstQuestionMark == -1)) {
            firstWildcard = firstStar;
        } else if ((firstQuestionMark < firstStar && firstQuestionMark >= 0) || (firstQuestionMark > -1 && firstStar == -1)) {
            firstWildcard = firstQuestionMark;
        }
        if (firstWildcard == -1) {
            return firstWildcard;
        }
        int lastSep = -1;
        int sep = pattern.indexOf(File.separator);
        if (sep > -1) {
            do {
                lastSep = sep;
                sep = pattern.indexOf(File.separator, sep + 1);
            } while (sep > 0 && sep < firstWildcard && sep < pattern.length());
        }
        return lastSep;
    }
    
    public static LinkedList<Path> getFileList(String pattern, int lastSeparator) {
        if (!pattern.contains("*") && !pattern.contains("?")) {
            return new LinkedList<Path>(Arrays.asList(Paths.get(pattern)));
        }
        String directory;
        String finalPattern;
        if (lastSeparator == -1) {
            directory = "./";
            finalPattern = pattern;
        } else {
            directory = pattern.substring(0, lastSeparator);
            finalPattern = pattern.substring(lastSeparator + 1);
        }
        
        class Finder extends SimpleFileVisitor<Path> {
            private final PathMatcher matcher;
            LinkedList<Path> files;
            private final String directory;
            
            Finder(String directory, String pattern) {
                this.matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
                this.files = new LinkedList<Path>();
                this.directory = directory;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Path name = file.getFileName();
                if (name != null && matcher.matches(name)) {
                    files.add(Paths.get(directory, name.toString()));
                }
                return FileVisitResult.CONTINUE;
            }
            
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }
            
            public FileVisitResult visitFileFailed(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
            
            public LinkedList<Path> getPathList() {
                return files;
            }
        }
        
        Finder finder = new Finder(directory, finalPattern);
        try {
            Files.walkFileTree(Paths.get(directory), finder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return finder.getPathList();
    }
}
