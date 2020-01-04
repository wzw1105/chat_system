package thread;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/*
 * 聊天客户端消息收发类
 */
public class ClientReceiveThread extends Thread {
	private JComboBox combobox;
	private JTextArea textarea;
	
	private static final String dir = "/Users/fuchen/Documents/";

	Socket socket;
	ObjectOutputStream output;
	ObjectInputStream input;
	JTextField showStatus;

	public ClientReceiveThread(Socket socket, ObjectOutputStream output, ObjectInputStream input, JComboBox combobox,
			JTextArea textarea, JTextField showStatus) {

		this.socket = socket;
		this.output = output;
		this.input = input;
		this.combobox = combobox;
		this.textarea = textarea;
		this.showStatus = showStatus;
	}

	public void run() {
		while (!socket.isClosed()) {
			try {
				String type = (String) input.readObject();

				if (type.equalsIgnoreCase("系统信息")) {
					String sysmsg = (String) input.readObject();
					textarea.append("系统信息: " + sysmsg);
				} else if (type.equalsIgnoreCase("服务关闭")) {
					output.close();
					input.close();
					socket.close();

					textarea.append("服务器已关闭！\n");

					break;
				} else if (type.equalsIgnoreCase("聊天信息")) {
					String message = (String) input.readObject();
					textarea.append(message);
				} else if (type.equalsIgnoreCase("用户列表")) {
					String userlist = (String) input.readObject();
					String usernames[] = userlist.split("\n");
					combobox.removeAllItems();

					int i = 0;
					combobox.addItem("所有人");
					while (i < usernames.length) {
						combobox.addItem(usernames[i]);
						i++;
					}
					combobox.setSelectedIndex(0);
					showStatus.setText("在线用户 " + usernames.length + " 人");
				} else if ("发送文件".equals(type)) {
					String fileName = (String) input.readObject();
					byte[] fileOrigin = (byte[]) input.readObject();
					textarea.append("收到文件" + fileName + ", 并且保存在" + dir + fileName);
					getFileByBytes(fileOrigin, dir, fileName);
				}
			} catch (Exception e) {
				System.out.println(e);
			}
		}
	}

	public static void getFileByBytes(byte[] bytes, String filePath, String fileName) {
		BufferedOutputStream bos = null;
		FileOutputStream fos = null;
		File file = null;
		try {
			File dir = new File(filePath);
			if (!dir.exists()) {// 判断文件目录是否存在
				dir.mkdirs();
			}
			file = new File(filePath + fileName);
			fos = new FileOutputStream(file);
			bos = new BufferedOutputStream(fos);
			bos.write(bytes);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static byte[] getBytesByFile(File file) {
		try {
			FileInputStream fis = new FileInputStream(file);
			ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
			byte[] b = new byte[1000];
			int n;
			while ((n = fis.read(b)) != -1) {
				bos.write(b, 0, n);
			}
			fis.close();
			byte[] data = bos.toByteArray();
			bos.close();
			return data;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
