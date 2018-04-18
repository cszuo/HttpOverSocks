package s3.configure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.json.JSONObject;

public class FileConfig {
	static String name = "config.json";

	public static void load() {
		// TODO Auto-generated method stub
		String path = FileConfig.class.getProtectionDomain().getCodeSource().getLocation().getPath() + File.separator
				+ name;
		File f = new File(path);
		if (!f.exists()) {

			PrintWriter writer;
			try {
				writer = new PrintWriter(f.getPath(), "UTF-8");
				writer.println(example().toString(2));
				writer.close();
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Configure.getInstance().print(FileConfig.class, "No configure file ( example config.json )");
			System.exit(0);
		}

		try {

			File file = f;
			FileInputStream fis = new FileInputStream(file);
			byte[] data = new byte[(int) file.length()];
			fis.read(data);
			fis.close();

			JSONObject js = new JSONObject(new String(data));
			System.out.println(js.toString());
			Configure.getInstance().setCofigJS(js);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static JSONObject example() {
		JSONObject js = new JSONObject();
		JSONObject tmp;
		js.put("proxy", "192.168.1.2:8080");

		tmp = new JSONObject();
		tmp.put("host", ".*.google.com");
		tmp.append("ports", "443");
		tmp.append("ports", "80");
		js.append("redirects", tmp);

		tmp = new JSONObject();
		tmp.put("host", ".*.baidu.com");
		tmp.append("ports", "443");
		js.append("redirects", tmp);
		tmp = new JSONObject();
		tmp.put("host", ".*");
		tmp.append("ports", ".*");
		js.append("redirects", tmp);

		return js;
	}

}
