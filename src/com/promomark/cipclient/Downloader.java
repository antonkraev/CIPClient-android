package com.promomark.cipclient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

public class Downloader {

	private static final String DOWNLOADER = "Downloader";

	public static final int CATEGORY_AR = 0;
	public static final int CATEGORY_DRINKS = 1;
	public static final int CATEGORY_COUPONS = 2;
	public static final int CATEGORY_CONTEST = 3;

	private Context context;
	private List<Item> queue;
	private FinishListener listener;
	private boolean categories[];

	interface FinishListener {
		public void downloadsDone(int category);
	}

	public static String convertToLocalName(String url) {
		String[] splitArray = url.split("/");
		return splitArray[splitArray.length - 1];
	}

	static class Item {
		int category;
		String url;
		String fileName;
		boolean done;

		public Item(Context context, int category, String url) {
			super();
			this.category = category;
			if (!url.startsWith("http://")) {
				url = "http://" + url;
			}
			url = url.replace("\\", "/");
			this.url = url;

			try {
				String fileName = convertToLocalName(url);
				this.fileName = context.getExternalFilesDir(null)
						+ File.separator + fileName;
				File file = new File(this.fileName);
				Log.i(DOWNLOADER, "Verifying: " + fileName);
				if (file.exists() && file.length() > 0) {
					// we just verify file is non-empty, we don't use HTTP HEAD to verify file is changed
					// if you need another file, give it another name
					Log.i(DOWNLOADER, "Cached file exists: " + fileName);
					this.done = true;
				} else {
					InputStream is = context.getResources().getAssets()
							.open(fileName);
					if (is != null) {
						Log.i(DOWNLOADER, "Resource found, copying: "
								+ fileName);
						FileOutputStream out = new FileOutputStream(
								this.fileName);
						byte[] buffer = new byte[1024];
						int read;
						while ((read = is.read(buffer)) != -1) {
							out.write(buffer, 0, read);
						}
						out.close();
					}
				}
			} catch (Exception e) {
				// broken item...
				Log.e(DOWNLOADER, "Cannot open " + url + " : " + e.getMessage());
				this.done = true;
			}
		}
	}

	public Downloader(Context context, FinishListener listener) {
		this.context = context;
		this.listener = listener;
		queue = new ArrayList<Downloader.Item>();
		categories = new boolean[CATEGORY_CONTEST + 1];
		for (int i = 0; i < categories.length; i++) {
			categories[i] = false;
		}
	}

	public boolean addItem(int category, String url) {
		Item item = new Item(context, category, url);
		if (item.done) {
			return true;
		} else {
			queue.add(item);
			return false;
		}
	}

	public void startDownloads() {
		notifyFinished();
		if (!queue.isEmpty()) {
			new Thread() {
				public void run() {
					while (queue.size() > 0) {
						Item toDownload = queue.remove(0);

						try {
							Log.i(DOWNLOADER, "starting: " + toDownload.url);
							URL url = new URL(toDownload.url);
							URLConnection connection = url.openConnection();
							connection.connect();
							InputStream input = new BufferedInputStream(
									url.openStream());
							OutputStream output = new FileOutputStream(
									toDownload.fileName);
							byte data[] = new byte[1024];
							int count;
							while ((count = input.read(data)) != -1) {
								output.write(data, 0, count);
							}

							output.flush();
							output.close();
							input.close();

							toDownload.done = true;
							Log.i(DOWNLOADER, "done: " + toDownload.url);
							notifyFinished();
						} catch (Exception e) {
							Log.e(DOWNLOADER, "problem downloading "
									+ toDownload.url + " : " + e.getMessage());
						}
					}
				}
			}.start();
		}
	}

	public boolean isCategoryReady(int category) {
		for (Item item : queue) {
			if (item.category == category) {
				return false;
			}
		}

		return true;
	}

	private void notifyFinished() {
		for (int i = CATEGORY_AR; i <= CATEGORY_CONTEST; i++) {
			if (isCategoryReady(i) && !categories[i]) {
				listener.downloadsDone(i);
				categories[i] = true;
			}
		}
	}
}
