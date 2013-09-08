import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.LinkedList;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.blinkenlights.jid3.ID3Exception;
import org.blinkenlights.jid3.MP3File;
import org.blinkenlights.jid3.MediaFile;
import org.blinkenlights.jid3.v2.APICID3V2Frame;
import org.blinkenlights.jid3.v2.ID3V2_3_0Tag;
import org.blinkenlights.jid3.v2.APICID3V2Frame.PictureType;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.soundcloud.api.ApiWrapper;

public class Downloder {

	private static final String CLIENT_SECRET = "8715282892a943e6c9472f27a098cf44";
	private static final String CLIENT_ID = "1658db91709a31648277029337106a74";
	public static final File WRAPPER_SER = new File("wrapper.ser");

	/**
	 * Downloads all sounds related to a given soundcloud profile.
	 * 
	 * @param args
	 *            dest=[destination] limit=[limit] profile=[profilename] goal=[goal]
	 * @throws IllegalAccessException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws URISyntaxException
	 * @throws ClassNotFoundException
	 * @throws JSONException
	 * @throws ParseException
	 * @throws ID3Exception
	 */
	public static void main(String[] args) throws IllegalAccessException,
			ClientProtocolException, IOException, URISyntaxException,
			ClassNotFoundException, JSONException, ParseException, ID3Exception {
		// parse arguments
		if (ArrayUtils.isEmpty(args) || args.length != 4) {
			throw new IllegalAccessException(
					"not all parameters set, call \"java jsoundcloud-downloader -dest=[destination] -limit=[limit] -profile=[profilename] -goal=[goal]");
		}

		String user = "drdaveli@yahoo.de";
		String password = "jsoundcloud-downloader";

		String[] destinationParam = StringUtils.split(args[0], "=");
		if (ArrayUtils.isEmpty(destinationParam)
				|| destinationParam.length != 2) {
			throw new IllegalAccessException(
					"parameter \"dest\" not correctly set");
		}
		File destination = new File(StringUtils.trim(destinationParam[1]));
		if (!destination.isDirectory()) {
			throw new IllegalAccessException(
					"parameter \"dest\" is not a valid directory");
		}

		if (!destination.exists()) {
			destination.mkdir();
			if (!destination.exists()) {
				throw new IllegalAccessException("directory \""
						+ destination.getAbsolutePath()
						+ "\" could not be created!");
			}
		}

		String[] limitParam = StringUtils.split(args[1], "=");
		if (ArrayUtils.isEmpty(limitParam) || limitParam.length != 2) {
			throw new IllegalAccessException(
					"parameter \"limit\" not correctly set");
		}
		int limit = Integer.valueOf(StringUtils.trim(limitParam[1]));

		String[] profileParam = StringUtils.split(args[2], "=");
		if (ArrayUtils.isEmpty(profileParam) || profileParam.length != 2) {
			throw new IllegalAccessException(
					"parameter \"profile\" not correctly set");
		}
		String profile = StringUtils.trim(profileParam[1]);

		String[] goalParam = StringUtils.split(args[3], "=");
		if (ArrayUtils.isEmpty(goalParam) || goalParam.length != 2) {
			throw new IllegalAccessException(
					"parameter \"goal\" not correctly set");
		}

		Goal goal = Goal.valueOf(StringUtils.trim(goalParam[1]).toUpperCase());
		if (goal == null) {
			throw new IllegalAccessException(
					"parameter \"goal\" not correctly set, must be one of LIKES, TRACKS or LIKES_AND_TRACKS");
		}

		// create needed directories
		File covers = new File(destination.getAbsolutePath() + "/covers");
		if (!covers.exists()) {
			covers.mkdir();
		}
		File sets = new File(destination.getAbsolutePath() + "/sets");
		if (!sets.exists()) {
			sets.mkdir();
		}

		// open connection and log in
		ApiWrapper wrapper = new ApiWrapper(CLIENT_ID, CLIENT_SECRET, null,
				null);
		wrapper.login(user, password);
		wrapper.toFile(WRAPPER_SER);

		// find out user id
		// String jsonProfile = ResourceFetcher.fetchResource("resolve", limit,
		// profile);
		// System.out.println(jsonProfile);
		//

		LinkedList<JSONObject> targets = new LinkedList<JSONObject>();

		// -- get all favorites
		if (Goal.LIKES == goal || Goal.LIKES_AND_TRACKS == goal) {
			String jsonTracks = ResourceFetcher.fetchResource("/users/"
					+ profile + "/favorites", Integer.MAX_VALUE, null);
			parseJsonTrackList(targets, jsonTracks);
		}

		// -- get all tracks
		if (Goal.TRACKS == goal || Goal.LIKES_AND_TRACKS == goal) {
			String jsonTracks = ResourceFetcher.fetchResource("/users/"
					+ profile + "/tracks", Integer.MAX_VALUE, null);
			parseJsonTrackList(targets, jsonTracks);
		}

		// download tracks
		for (int i = 0; i < targets.size(); i++) {
			if (i == limit) {
				return;
			}

			System.out.println("(" + (i + 1) + "/"
					+ Math.min(limit, targets.size()) + ") Downloading \""
					+ targets.get(i).get("title") + "\"");
			downloadTrack(destination, targets.get(i), targets.size() - i);
			System.out.println("Finished download.");
		}
	}

	private static void parseJsonTrackList(LinkedList<JSONObject> targets,
			String jsonTracks) throws ParseException {
		JSONArray tracks = (JSONArray) new JSONParser().parse(jsonTracks);
		for (@SuppressWarnings("unchecked")
		Iterator<JSONObject> iterator = tracks.iterator(); iterator.hasNext();) {
			JSONObject track = (JSONObject) iterator.next();
			targets.add(track);
		}
	}

	private static void downloadTrack(File destination, JSONObject track,
			int index) throws MalformedURLException, IOException, ID3Exception {
		try {
			String streamUrl = (String) track.get("stream_url") + "?client_id="
					+ CLIENT_ID;
			String title = index + " - " + (String) track.get("title");
			String description = (String) track.get("description");
			String permalink = (String) track.get("permalink");
			String username = (String) ((JSONObject) track.get("user"))
					.get("username");
			String artwork = (String) track.get("artwork_url");

			System.out.println(streamUrl);

			URLConnection conn = new URL(streamUrl).openConnection();
			InputStream is = conn.getInputStream();

			File file = new File(destination.getAbsolutePath() + "/" + permalink + ".mp3");
			File setPath = new File(destination.getAbsolutePath() + "/sets/"
					+ index + "-" + permalink + ".mp3");
			if (file.exists() || setPath.exists()) {
				System.out
						.println(".. already downloaded this one, skipping ..");
				return;
			}

			OutputStream outstream = new FileOutputStream(file);
			byte[] buffer = new byte[4096];
			int len;
			while ((len = is.read(buffer)) > 0) {
				outstream.write(buffer, 0, len);
			}
			outstream.close();

			// set mp3 tags
			MediaFile mp3 = new MP3File(file);

			ID3V2_3_0Tag oID3V2_3_0Tag = new ID3V2_3_0Tag();

			oID3V2_3_0Tag.setTitle(title);
			oID3V2_3_0Tag.setArtist(username);
			oID3V2_3_0Tag.setComment(description);
			oID3V2_3_0Tag.setAlbum(permalink);

			boolean isSet = file.length() / 1024 > 10000;
			if (isSet) {
				oID3V2_3_0Tag.setGenre("Soundcloud Sets");
			} else {
				oID3V2_3_0Tag.setGenre("Soundcloud Tracks");
			}

			// picture
			if (!StringUtils.isEmpty(artwork)) {
				// JPG
				try {
					artwork = StringUtils.replace(artwork, "large", "original");
					System.out.println(artwork);
					BufferedImage artworkImage = ImageIO.read(new URL(artwork));
					ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
					ImageIO.write(artworkImage, "jpeg", byteArrayOutputStream);
					byte[] imageData = byteArrayOutputStream.toByteArray();
					APICID3V2Frame image = new APICID3V2Frame("image/jpeg",
							PictureType.FrontCover, "", imageData);
					oID3V2_3_0Tag.addAPICFrame(image);

					File outputfile = new File(destination.getAbsolutePath()
							+ "/covers/" + permalink + ".jpg");
					ImageIO.write(artworkImage, "jpeg", outputfile);

				} catch (Exception ex) {
					// PNG
					try {
						artwork = StringUtils.replace(artwork, "jpg", "png");
						artwork = StringUtils.replace(artwork, "jpeg", "png");
						System.out.println(artwork);
						BufferedImage artworkImage = ImageIO.read(new URL(
								artwork));
						ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
						ImageIO.write(artworkImage, "png",
								byteArrayOutputStream);
						byte[] imageData = byteArrayOutputStream.toByteArray();
						APICID3V2Frame image = new APICID3V2Frame("image/png",
								PictureType.FrontCover, "", imageData);
						oID3V2_3_0Tag.addAPICFrame(image);

						File outputfile = new File(
								destination.getAbsolutePath() + "/covers/"
										+ permalink + ".png");
						ImageIO.write(artworkImage, "png", outputfile);

					} catch (Exception ex2) {
						ex2.printStackTrace();
					}
				}
			}

			// set this v2.3.0 tag in the media file object
			mp3.setID3Tag(oID3V2_3_0Tag);

			// update the actual file to reflect the current state of our object
			mp3.sync();

			// check if it is a set
			if (isSet) {
				file.renameTo(new File(destination.getAbsolutePath() + "/sets/"
						+ file.getName()));
			}

		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	private enum Goal {
		LIKES, TRACKS, LIKES_AND_TRACKS;
	}
}
