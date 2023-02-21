package com.techreturners.DAO;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.ClassroomScopes;
import com.google.api.services.classroom.model.*;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ClassroomDAO {
    private static final String APPLICATION_NAME = "Google Classroom API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String JAVA_COURSE_ID = "579045242269";

    private static final List<String> SCOPES =
            List.of(ClassroomScopes.CLASSROOM_COURSES_READONLY,
                    ClassroomScopes.CLASSROOM_ROSTERS_READONLY,
                    ClassroomScopes.CLASSROOM_COURSEWORK_STUDENTS,
                    ClassroomScopes.CLASSROOM_TOPICS);

    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    public ClassroomDAO() throws GeneralSecurityException, IOException {
    }

    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = ClassroomDAO.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    // Build a new authorized API client service.
    Classroom service =
            new Classroom.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

    public List<Course> getCourses() throws IOException, GeneralSecurityException {

        ListCoursesResponse response = service.courses().list()
                .setPageSize(10)
                .execute();
        List<Course> courses = response.getCourses();
        if (courses == null || courses.size() == 0) {
            System.out.println("No courses found.");
        }
        return courses;
    }

    public List<Student> getStudentList(String courseId) throws IOException, GeneralSecurityException {
        ListStudentsResponse response2 = service.courses().students().list(courseId)
                .setPageSize(30)
                .execute();
        List<Student> students = response2.getStudents();
        if (students == null || students.size() == 0) {
            System.out.println("No students found.");
        }
        return students;
    }

    public List<Topic> getTopics(String courseId) throws IOException, GeneralSecurityException {
        List<Topic> topics = new ArrayList<>();
        String pageToken = null;
        try {
            do {
                ListTopicResponse listTopicResponseesponse =
                        service
                                .courses()
                                .topics()
                                .list(courseId)
                                .setPageSize(100)
                                .setPageToken(pageToken)
                                .execute();

                /* Ensure that the response is not null before retrieving data from it to avoid errors. */
                if (listTopicResponseesponse.getTopic() != null) {
                    topics.addAll(listTopicResponseesponse.getTopic());
                    pageToken = listTopicResponseesponse.getNextPageToken();
                }
            } while (pageToken != null);

            if (topics.isEmpty()) {
                System.out.println("No topics found.");
            }
        } catch (GoogleJsonResponseException e) {
            // TODO (developer) - handle error appropriately
            GoogleJsonError error = e.getDetails();
            System.out.println(error);
            if (error.getCode() == 404) {
                System.out.printf("The courseId does not exist: %s.\n", courseId);
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw e;
        }
        return topics;
    }
}