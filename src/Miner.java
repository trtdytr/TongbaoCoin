import java.util.ArrayList;

public class Miner extends Thread {
	public static boolean solutionClaimed[];
	public static int claimerID[];
	public static int candidate[];
	public static ArrayList<ArrayList<Boolean>> consensusList = new ArrayList<ArrayList<Boolean>>();
	public static int minerNum = 0;
	public static String final_nounce[];
	public static int shardsNum;
	public static int minersPerShard;

	private int difficulty;
	private long prevInfo;
	private int shardID;

	public int index;
	public String solution;

	public Miner(String minerID, long prevInfo, int difficulty, int shardID) {
		super(minerID);
		index = minerNum;
		minerNum++;
//		this.shardID = index % shardsNum;
//		consensusList.get(shardID).add(false);
		solution = "";
		this.prevInfo = prevInfo;
		this.difficulty = difficulty;
		this.shardID = shardID;
		consensusList.get(shardID).add(false); // add a default element to consensusList of shard[shardID]
//		System.out.println("Creating miner thread: " + minerID);
	}

	public static void prefigureSharding(int num, int minersPerShard) {
		Miner.minersPerShard = minersPerShard;
		minerNum = 0;
		shardsNum = num;
		solutionClaimed = new boolean[shardsNum];
		claimerID = new int[shardsNum];
		candidate = new int[shardsNum];
		final_nounce = new String[shardsNum];

		consensusList.removeAll(consensusList);
		for (int i = 0; i < shardsNum; i++) {
			consensusList.add(new ArrayList<Boolean>());
			solutionClaimed[i] = false;
			claimerID[i] = -1;
			candidate[i] = Integer.MIN_VALUE;
			final_nounce[i] = "";
		}

	}

	@Override
	public void run() {
//		System.out.println("Running miner thread: " + this.getName());
		int nounce = Integer.MIN_VALUE;
		while (!consensusAchieved()) {
			while (!solutionClaimed[shardID]
					&& !numLeading0is(difficulty, Encryption.sha256("" + nounce + prevInfo + shardID))) {
				nounce++;
				if (nounce == Integer.MAX_VALUE
						&& !numLeading0is(difficulty, Encryption.sha256("" + nounce + prevInfo + shardID))) {
					prevInfo++;
					nounce = Integer.MIN_VALUE;
				}
			}
			if (solutionClaimed[shardID]) { // if someone in the same shard claims an solution, verify that
				if (numLeading0is(difficulty, Encryption.sha256("" + candidate[this.shardID] + prevInfo + shardID))) {
					consensusList.get(shardID).set(index % minersPerShard, true);
				} else {
					// if this candidate fails the verification
					resetShardConsensus();
				}
				// TODO1: verify the claimed solution
				// TODO2: report your verification to the public
			} else if (numLeading0is(difficulty, Encryption.sha256("" + nounce + prevInfo + shardID))) {
				// if this miner finds a solution, report to the public, and wait for
				// verification
				solutionClaimed[this.shardID] = true;
				consensusList.get(this.shardID).set(index % minersPerShard, true);
				candidate[this.shardID] = nounce;
				claimerID[this.shardID] = index;
			}
		}
		final_nounce[this.shardID] = "" + candidate[this.shardID] + prevInfo + shardID;
		System.out.println("Miner" + (this.index + 1) + "(" + this.getName() + ")" + " has approved that Miner"
				+ (claimerID[this.shardID] + 1) + " came up with the correct solution: " + "\""
				+ final_nounce[this.shardID] + "\"");

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

	private void resetShardConsensus() {
		ArrayList<Boolean> thisList = consensusList.get(this.shardID);
		// 1, reset the consensusList to all false
		for (int i = 0; i < thisList.size(); i++) {
			thisList.set(i, false);
		}
		// 2, reset candidate
		candidate[this.shardID] = Integer.MIN_VALUE;
		// 3, reset solutionClaimed to false
		solutionClaimed[this.shardID] = false;
		// 4, reset claimerID to -1
		claimerID[this.shardID] = -1;
	}

	private boolean consensusAchieved() {
		ArrayList<Boolean> thisList = consensusList.get(this.shardID);
		boolean agree = true;
		for (int i = 0; i < thisList.size(); i++) {
			if (thisList.get(i) == false) {
				agree = false;
				break;
			}
		}
		return agree;
	}
}
