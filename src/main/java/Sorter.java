import java.io.File;

import org.blinkenlights.jid3.ID3Exception;
import org.blinkenlights.jid3.MP3File;
import org.blinkenlights.jid3.MediaFile;
import org.blinkenlights.jid3.v2.ID3V2_3_0Tag;


public class Sorter {

	public static void main(String[] args) throws ID3Exception {
		
		File dir = new File("F:/downloads/soundcloud");
		File sets = new File("F:/downloads/soundcloud/sets");
		if (!sets.exists()) {
			sets.mkdir();
		}

		File[] files = dir.listFiles();
		for (File file : files) {
			if (!file.getName().contains("mp3")) {
				continue;
			}

			MediaFile mp3 = new MP3File(file);
			if (((ID3V2_3_0Tag) mp3.getTags()[0]).getGenre().contains("Sets")) {
				file.renameTo(new File("F:/downloads/soundcloud/sets/"
						+ file.getName()));
			}
		}
		
		
	}
	
}
