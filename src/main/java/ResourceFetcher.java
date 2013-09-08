import java.io.File;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Http;
import com.soundcloud.api.Request;

/**
 * Access a protected resource from the server. This needs a prepared
 * (serialized) API wrapper instance (create one with CreateWrapper).
 * 
 * @see CreateWrapper
 */
public final class ResourceFetcher {

	public static String fetchResource(String res, int limit, String url)
			throws IOException, ClassNotFoundException {
		final File wrapperFile = Downloder.WRAPPER_SER;

		if (!wrapperFile.exists()) {
			throw new IllegalArgumentException("\nThe serialised wrapper ("
					+ wrapperFile + ") does not exist.\n"
					+ "Run CreateWrapper first to create it.");
		}
		final ApiWrapper wrapper = ApiWrapper.fromFile(wrapperFile);
		final Request resource = Request.to(res);
		resource.add("limit", Integer.MAX_VALUE);
		if (url != null) {
			resource.add("url", url);
		}
		System.out.println("GET " + resource);
		try {
			HttpResponse resp = wrapper.get(resource);
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				String json = Http.formatJSON(Http.getString(resp));
				return json;
			} else {
				throw new IllegalArgumentException("Invalid status received: "
						+ resp.getStatusLine());
			}
		} finally {
			// serialise wrapper state again (token might have been
			// refreshed)
			wrapper.toFile(wrapperFile);
		}
	}

}
