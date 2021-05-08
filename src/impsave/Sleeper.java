package impsave;

public class Sleeper implements Runnable {
	private int millis;

	public Sleeper(int millis) {
		this.millis = millis;
	}

	@Override
	public void run() {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
