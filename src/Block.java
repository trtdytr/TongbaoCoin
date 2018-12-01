import java.util.Date;

import lombok.Getter;

@Getter
public class Block {
	private int index;
	private long timestamp;
	private String note;
	private Transaction transactions[];
	private String prev_hash;
	private String hash;
	private int difficulty;
	private String nounce;

	public static final int BLOCK_SIZE = 20;

	public Block(int index, String prev_hash, String nounce, int difficulty) {
		this.index = index;
		this.timestamp = new Date().getTime();
		this.prev_hash = prev_hash;
		this.nounce = nounce;
		this.difficulty = difficulty;
		transactions = new Transaction[BLOCK_SIZE];
	}

	public void setNote(String note) {
		this.note = note;
	}

	public void setTransactions(Transaction[] transactions) {
		// make a deep copy
		for (int i = 0; i < this.transactions.length; i++) {
			this.transactions[i] = transactions[i];
		}
	}

	public String toString() {
		return ("Block #" + this.index + "\n\tmined at: " + this.timestamp + "\n\tNote: " + this.note
				+ "\n\tTransactions: " + this.transactions + "\n\tHash: {" + this.hash + "}\n");
	}

	private String calcHash() {
		return Encryption.sha256(this.index + this.timestamp + this.note + this.transactions + this.prev_hash);
	}
	
	public void setHash() {
		hash = calcHash();
	}
}
