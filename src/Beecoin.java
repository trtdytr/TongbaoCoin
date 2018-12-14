import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.GsonBuilder;

public class Beecoin {
	private static ArrayList<Block> blockchain = new ArrayList<>(); // The blockchain is implemented as an arraylist of
																	// Blocks
	private static ArrayList<Transaction> transactions = new ArrayList<>();
	private static ArrayList<String> miners_address = new ArrayList<>();
	private static ArrayList<String> coinHolders_address = new ArrayList<>(); // a list keeps track of all coin holders

	private static int confirmedTxions_count = 0;
	private static int verifiedTxions_count = 0;

	private final static int MINERS_NUM = 4;
	private final static int MAX_BLOCKS = 10;
	private final static int DIFFICULTY = 4;
	private final static double MINING_REWARDS = 6;
	private static final int MAX_TXIONS_EACH_PERSON_EACH_EPOCH = 5;

	public static void main(String args[]) {
		long startTime, endTime, totalTime;
		Block newBlock = createGenesisBlock();
		blockchain.add(newBlock);
		miners_address = loadMiners();

		totalTime = 0;

		for (int i = 0; i < MAX_BLOCKS; i++) {
			simulateTransactions();
			startTime = System.currentTimeMillis();
			newBlock = mine();
			endTime = System.currentTimeMillis();
			totalTime += (endTime - startTime);
			blockchain.add(newBlock);
			updateCoinHolders(newBlock);
		}

		String blockchainJson = new GsonBuilder().setPrettyPrinting().create().toJson(blockchain);
		System.out.println(blockchainJson);

		printStats(totalTime);
	}

	private static void printStats(long totalTime) {
		System.out.println("\nBlock mining rate: " + roundToN((MAX_BLOCKS / (totalTime / 1000.0)), 2) + " Blocks/sec");

		System.out.println("\nAll minted coins: " + MAX_BLOCKS * MINING_REWARDS);
		double coinsOfHolders = 0;
		for (int i = 0; i < coinHolders_address.size(); i++) {
			coinsOfHolders += getBalance(coinHolders_address.get(i));
		}
		System.out.println("All coins in coinHolders: " + roundToN(coinsOfHolders, 2) + "\n");

		printCoinHolders();

		System.out.println("\nUnconfirmed transactions:\n" + transactions);

		System.out.println("\nTransactions confirming rate: "
				+ roundToN((confirmedTxions_count - transactions.size()) / (totalTime / 1000.0), 2)
				+ " Transactions/sec");
		System.out.println("Transactions verifying rate: "
				+ roundToN((verifiedTxions_count - transactions.size()) / (totalTime / 1000.0), 2)
				+ " Transactions/sec");
	}

	public static Block createGenesisBlock() {
		Block genesisBlock = new Block(0, "0", "no nounce for the first time", DIFFICULTY);
		genesisBlock.setNote("Everything starts from here!");
		return genesisBlock;
	}

	public static Block createNextBlock(Block prevBlock, String nounce) {
		return new Block(prevBlock.getIndex() + 1, prevBlock.getHash(), nounce, DIFFICULTY);
	}

	// this PoW algorithm try to find an integer 'nounce',
	// so that sha256(nounce+previous_block.timestamp) contains the required number
	// of leading 0's.
	public static String proofOFwork(long prevTimestamp) {
		int nounce = Integer.MIN_VALUE;
		while (!numLeading0is(DIFFICULTY, Encryption.sha256("" + nounce + prevTimestamp))) {
			nounce++;
			if (nounce == Integer.MAX_VALUE
					&& !numLeading0is(DIFFICULTY, Encryption.sha256("" + nounce + prevTimestamp))) {
				prevTimestamp++;
				nounce = Integer.MIN_VALUE;
			}
		}

		return ("" + nounce + prevTimestamp);
	}

	// this method verifies that whether the giving hash contains the given amount
	// of leading 0's
	public static boolean numLeading0is(int amount, String hash) {
		boolean result = true;
		int count = 0;
		for (int i = 0; i < hash.length(); i++) {
			if (hash.charAt(i) == '0') {
				count++;
			} else {
				break;
			}
		}
		if (count != amount) {
			result = false;
		}

		return result;
	}

	public static Block mine() {
		Block lastBlock = blockchain.get(blockchain.size() - 1);
		String miner_address = getMinerAddr();
		String nounce;
		nounce = proofOFwork(lastBlock.getTimestamp());
		Block next = createNextBlock(lastBlock, nounce);

		Transaction nextToBeConfirmed[] = new Transaction[Block.BLOCK_SIZE];

		// rewards to the miner will be the first txion
		nextToBeConfirmed[0] = new Transaction("System", miner_address, MINING_REWARDS, true);
		retreiveVerifiedTxions(nextToBeConfirmed);

		next.setTransactions(nextToBeConfirmed);
		next.setNote("This is Block #" + next.getIndex());

		return next;
	}

	public static void retreiveVerifiedTxions(Transaction[] nextToBeConfirmed) {
		HashMap<String, Double> tempBalanceMap = new HashMap<String, Double>();
		int i = 1;
		while (nextToBeConfirmed[nextToBeConfirmed.length - 1] == null && !transactions.isEmpty()) {
			Transaction curr = transactions.get(0);
			String sender = curr.getSenderID();
			double balance;
			if (tempBalanceMap.containsKey(sender)) {
				balance = tempBalanceMap.get(sender);
			} else {
				balance = getBalance(sender);
				tempBalanceMap.put(sender, balance);
			}
			if (balance < curr.getAmount() || curr.getAmount() == 0.0) {
				curr.setVerified(false);
			} else {
				curr.setVerified(true);
				nextToBeConfirmed[i] = new Transaction(curr.getSenderID(), curr.getRecepientID(), curr.getAmount(),
						true);
				i++;
				confirmedTxions_count++;
				balance -= curr.getAmount();
				tempBalanceMap.put(sender, balance);
			}
			transactions.remove(curr);
			verifiedTxions_count++;
		}
	}

	public static ArrayList<String> loadMiners() {
		ArrayList<String> miners = new ArrayList<>();
		String address = null;

		for (int i = 0; i < MINERS_NUM; i++) {
			address = Encryption.sha256("miner" + i);
			miners.add(address);
		}

		return miners;
	}

	public static String getMinerAddr() {
		return miners_address.get((int) ((Math.random() * MINERS_NUM)));
	}

	public static void addCoinHolder(String addr) {
		if (!coinHolders_address.contains(addr)) {
			coinHolders_address.add(addr);
		}
	}

	public static void updateCoinHolders(Block block) {
		for (int i = 0; i < Block.BLOCK_SIZE; i++) {
			Transaction curr = block.getTransactions()[i];
			if (curr == null) {
				break;
			} else {
				addCoinHolder(curr.getRecepientID());
			}
		}
	}

	public static void simulateTransactions() {
		// only work when at least one person holds some coins
		if (!coinHolders_address.isEmpty()) {
			int numTxions = 0;

			for (int i = 0; i < coinHolders_address.size(); i++) {
				numTxions += (int) (MAX_TXIONS_EACH_PERSON_EACH_EPOCH * Math.random());
			}

			for (int i = 0; i < numTxions; i++) {
				simulateAtransaction();
			}
		}
	}

	public static void simulateAtransaction() {
		// randomly pick an sender
		String sender = getSender();
		double issBallence = getBalance(sender);
		double amount = ((int) (issBallence * Math.random() * 100)) / 100.0;
		String recepient = getRecepient(sender);
		transactions.add(new Transaction(sender, recepient, amount));
	}

	public static String getSender() {
		return coinHolders_address.get((int) ((Math.random() * coinHolders_address.size())));
	}

	public static String getRecepient(String sender) {
		String recepient = null;
		double isNewUser = Math.random();
		if (coinHolders_address.size() == 1 || isNewUser < 0.5) {
			recepient = Encryption.sha256("coinHolder" + coinHolders_address.size() + 1);
		} else {
			do {
				recepient = coinHolders_address.get((int) ((Math.random() * coinHolders_address.size())));
			} while (recepient == null || recepient.equals(sender));
		}

		return recepient;
	}

	public static double getBalance(String addr) {
		double balance = 0;
		for (int i = 1; i < blockchain.size(); i++) {
			Block currB = blockchain.get(i);
			for (int j = 0; j < Block.BLOCK_SIZE; j++) {
				Transaction currT = currB.getTransactions()[j];
				if (currT == null) {
					break;
				}

				if (currT.getRecepientID().equals(addr)) {
					balance += currT.getAmount();
				} else if (currT.getSenderID().equals(addr)) {
					balance -= currT.getAmount();
				}

			}
		}

		return (Math.round(balance * 100)) / 100.0;
	}

	public static void printCoinHolders() {
		for (int i = 0; i < coinHolders_address.size(); i++) {
			String addr = coinHolders_address.get(i);
			System.out.println(addr + " owns: " + getBalance(addr));
		}

	}

	public static void printChain() {
		for (int i = 0; i < blockchain.size(); i++) {
			System.out.println(blockchain.get(i));
		}
	}

	public static double roundToN(double origin, int n) {
		return Math.round(origin * Math.pow(10, n)) / ((double) Math.pow(10, n));
	}
}
