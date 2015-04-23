package org.kotemaru.android.bizcard.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;

public class CardImageUtil {
	private static final int FF = 0x0ff;
	private static final float CARD_ASPECT = 1.654545454545455F;

	public static String saveThumbnail(Context context, int id, Bitmap bitmap) throws IOException {
		bitmap = toThumbnail(bitmap);
		File file = getThumbnailFile(context, id);
		file.getParentFile().mkdirs();
		FileOutputStream out = new FileOutputStream(file);
		try {
			bitmap.compress(CompressFormat.PNG, 50, out);
		} finally {
			out.close();
		}
		return getThumbnailUrl(id);
	}
	public static Bitmap loadThumbnail(Context context, int id) {
		File file = getThumbnailFile(context, id);
		if (!file.canRead()) return null;
		try {
			FileInputStream in = new FileInputStream(file);
			try {
				Bitmap bitmap = BitmapFactory.decodeStream(in);
				return bitmap;
			} finally {
				in.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static String getThumbnailUrl(int id) {
		return String.format(Locale.US,
				"file:///data/data/thumbnail/%010d.png", Integer.valueOf(id));
	}

	public static File getThumbnailFile(Context context, int id) {
		File dataDir = context.getFilesDir();
		String idStr = String.format(Locale.US, "%010d", Integer.valueOf(id));
		File file = new File(dataDir, "thumbnail/" + idStr + ".png");
		return file;
	}

	public static Bitmap toThumbnail(Bitmap bitmap) {
		int height = bitmap.getHeight();
		int width = bitmap.getWidth();
		if (width > height) {
			width = 1024;
			height = (int) (1024 / CARD_ASPECT);
		} else {
			width = (int) (1024 / CARD_ASPECT);
			height = 1024;
		}

		bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
		int[] pixels = new int[width * height];
		bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

		int maxGray = 0;
		int minGray = 255;
		for (int y = 0; y < width; ++y) {
			for (int x = 0; x < height; ++x) {
				int index = (y + x * width);
				int argb = pixels[index];
				int gray = ((argb >> 16 & FF) + (argb >> 8 & FF) + (argb & FF)) / 3;
				minGray = gray < minGray ? gray : minGray;
				maxGray = gray > maxGray ? gray : maxGray;
				pixels[index] = gray;
			}
		}

		int base = (maxGray - minGray) / 16;
		if (base == 0) base = 1;
		for (int i = 0; i < pixels.length; i++) {
			int b4 = ((pixels[i] - minGray) / base) << 4 | 0x0F;
			b4 = b4 > 255 ? 255 : b4;
			pixels[i] = (FF << 24) | (b4 << 16) | (b4 << 8) | b4;
		}

		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
	}

}
