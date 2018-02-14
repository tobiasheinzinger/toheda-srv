package com.toheda.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.amazonaws.util.IOUtils;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.ListUsersPage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.toheda.server.model.ContactModel;
import com.toheda.server.model.ProcessModel;
import com.toheda.server.model.UserModel;

@RestController
@RequestMapping("/services")
public class ServiceController {

	private final ResourceLoader resourceLoader;

	@Autowired
	public ServiceController(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;

		Resource resource = resourceLoader
				.getResource("classpath:toheda-253e0-firebase-adminsdk-ggz69-878be7c740.json");

		FirebaseOptions options;
		try {
			options = new FirebaseOptions.Builder()
					.setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
					.setDatabaseUrl("https://toheda-253e0.firebaseio.com/").build();
			FirebaseApp.initializeApp(options);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/process-list", method = RequestMethod.POST)
	public ResponseEntity<List<ProcessModel>> processes(@RequestBody(required = false) UserModel userModel) {

		List<ProcessModel> processes = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			processes.add(new ProcessModel("ID" + (i + 1), "Process " + (i + 1)));
		}

		return new ResponseEntity<List<ProcessModel>>(processes, HttpStatus.OK);
	}

	@RequestMapping(value = "/process-image", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
	public @ResponseBody byte[] processImage(@RequestParam String processId, HttpServletResponse resonse)
			throws IOException {

		String imageName = "process.png";
		if (StringUtils.equals("ID1", processId)) {
			imageName = "einkaufsliste.png";

		} else if (StringUtils.equals("ID4", processId)) {
			imageName = "fussball.png";
		}

		Resource resource = resourceLoader.getResource("classpath:" + imageName);
		BufferedInputStream inputStream = new BufferedInputStream(resource.getInputStream());

		return IOUtils.toByteArray(inputStream);
	}

	@RequestMapping(value = "/contacts", method = RequestMethod.POST)
	public ResponseEntity<List<String>> contacts(@RequestBody(required = true) List<ContactModel> contacts,
			HttpServletRequest request) throws IOException, InterruptedException, ExecutionException {
		List<String> ids = new ArrayList<>();
		ListUsersPage page = FirebaseAuth.getInstance().listUsersAsync(null).get();
		while (page != null) {
			for (ExportedUserRecord user : page.getValues()) {
				for (ContactModel contact : contacts) {
					if (StringUtils.equals(contact.getEmail(), user.getEmail())) {
						ids.add(contact.getId());
					}
				}
			}

			page = page.getNextPage();
		}

		return new ResponseEntity<List<String>>(ids, HttpStatus.OK);
	}

	@RequestMapping(value = "/notification", method = RequestMethod.POST)
	public void notification(@RequestBody(required = true) UserModel user) throws IOException {
		URL url = new URL("https://fcm.googleapis.com/v1/projects/toheda-253e0/messages:send");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);

		JsonObject notification = new JsonObject();
		notification.addProperty("title", "Notification from ToHeDA");
		notification.addProperty("body", "Hi there,\nsomething's going on @ ToHeDA.\nPlease have a look");
		notification.addProperty("sound", "default");

		JsonObject android = new JsonObject();
		android.add("notification", notification);

		JsonObject message = new JsonObject();
		message.addProperty("token", user.getClientToken());
		message.add("android", android);

		JsonObject payload = new JsonObject();
		payload.add("message", message);

		Gson gson = new GsonBuilder().create();
		String json = gson.toJson(payload);

		OutputStream outputStream = connection.getOutputStream();
		outputStream.write(json.getBytes());
		outputStream.close();

		// Exception happen here
		InputStream inputStream = connection.getInputStream();

		inputStream.close();
		connection.disconnect();
	}

	private String getAccessToken() throws IOException {
		Resource resource = resourceLoader
				.getResource("classpath:toheda-253e0-firebase-adminsdk-ggz69-878be7c740.json");

		GoogleCredential googleCredential = GoogleCredential.fromStream(resource.getInputStream())
				.createScoped(Arrays.asList("https://www.googleapis.com/auth/firebase.messaging"));
		googleCredential.refreshToken();

		return googleCredential.getAccessToken();
	}
}