package thread;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import entity.Node;
import entity.UserLinkList;

/*
 * 服务器接收消息的类
 */
public class ServerReceiveThread extends Thread {
	JTextArea textarea;
	JTextField textfield;
	JComboBox combobox;
	Node client;
	UserLinkList userLinkList;// 用户链表

	public boolean isStop;

	public ServerReceiveThread(JTextArea textarea, JTextField textfield, JComboBox combobox, Node client,
			UserLinkList userLinkList) {

		this.textarea = textarea;
		this.textfield = textfield;
		this.client = client;
		this.userLinkList = userLinkList;
		this.combobox = combobox;

		isStop = false;
	}

	public void run() {
		// 向所有人发送用户的列表
		sendUserList();

		while (!isStop && !client.getSocket().isClosed()) {
			try {
				String type = (String) client.getInput().readObject();

				if (type.equalsIgnoreCase("聊天信息")) {
					String toSomebody = (String) client.getInput().readObject();
					String status = (String) client.getInput().readObject();
					String action = (String) client.getInput().readObject();
					String message = (String) client.getInput().readObject();

					String msg = client.getUserName() + " " + action + "对 " + toSomebody + " 说 : " + message + "\n";
					if (status.equalsIgnoreCase("悄悄话")) {
						msg = " [悄悄话] " + msg;
					}
					textarea.append(msg);

					if (toSomebody.equalsIgnoreCase("所有人")) {
						sendToAll(msg);// 向所有人发送消息
					} else {
						try {
							client.getOutput().writeObject("聊天信息");
							client.getOutput().flush();
							client.getOutput().writeObject(msg);
							client.getOutput().flush();
						} catch (Exception e) {
						}

						Node node = userLinkList.findUser(toSomebody);

						if (node != null) {
							node.getOutput().writeObject("聊天信息");
							node.getOutput().flush();
							node.getOutput().writeObject(msg);
							node.getOutput().flush();
						}
					}
				} else if (type.equalsIgnoreCase("用户下线")) {
					Node node = userLinkList.findUser(client.getUserName());
					userLinkList.delUser(node);

					String msg = "用户 " + client.getUserName() + " 下线\n";
					int count = userLinkList.getCount();

					combobox.removeAllItems();
					combobox.addItem("所有人");
					int i = 0;
					while (i < count) {
						node = userLinkList.findUser(i);
						if (node == null) {
							i++;
							continue;
						}

						combobox.addItem(node.getUserName());
						i++;
					}
					combobox.setSelectedIndex(0);

					textarea.append(msg);
					textfield.setText("在线用户" + userLinkList.getCount() + "人\n");

					sendToAll(msg);// 向所有人发送消息
					sendUserList();// 重新发送用户列表,刷新

					break;
				} else if ("发送文件".equals(type)) {
					String toSomebody = (String) client.getInput().readObject();
					String fileName = (String) client.getInput().readObject();
					byte[] fileOrigin = (byte[]) client.getInput().readObject();
					Node node = userLinkList.findUser(toSomebody);
					
					node.getOutput().writeObject("发送文件");
					node.getOutput().flush();
					node.getOutput().writeObject(fileName);
					node.getOutput().flush();
					node.getOutput().writeObject(fileOrigin);
					node.getOutput().flush();
				}
			} catch (Exception e) {
				System.out.println(e);
			}
		}
	}

	/*
	 * 向所有人发送消息
	 */
	public void sendToAll(String msg) {
		int count = userLinkList.getCount();

		int i = 0;
		while (i < count) {
			Node node = userLinkList.findUser(i);
			if (node == null) {
				i++;
				continue;
			}

			try {
				node.getOutput().writeObject("聊天信息");
				node.getOutput().flush();
				node.getOutput().writeObject(msg);
				node.getOutput().flush();
			} catch (Exception e) {
				// System.out.println(e);
			}

			i++;
		}
	}

	/*
	 * 向所有人发送用户的列表
	 */
	public void sendUserList() {
		String userlist = "";
		int count = userLinkList.getCount();

		int i = 0;
		while (i < count) {
			Node node = userLinkList.findUser(i);
			if (node == null) {
				i++;
				continue;
			}

			userlist += node.getUserName();
			userlist += '\n';
			i++;
		}

		i = 0;
		while (i < count) {
			Node node = userLinkList.findUser(i);
			if (node == null) {
				i++;
				continue;
			}

			try {
				node.getOutput().writeObject("用户列表");
				node.getOutput().flush();
				node.getOutput().writeObject(userlist);
				node.getOutput().flush();
			} catch (Exception e) {
				// System.out.println(e);
			}
			i++;
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
