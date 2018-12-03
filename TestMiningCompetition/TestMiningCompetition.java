import java.util.ArrayList;

public class TestMiningCompetition {
  private final static int MINER_NUM = 4;
  private static String nounce; 
  
  public static void main(String[] args) {
    ArrayList<String> miners = generateMiners(MINER_NUM);
    for(int i = 0; i < miners.size(); i++) {
      MinerThread mt = new MinerThread(miners.get(i));
      mt.start();
    }
    
  }
  
  public static ArrayList<String> generateMiners(int minerNum) {
    ArrayList<String> minerList = new ArrayList<String>();
    for(int i = 0; i < minerNum; i++) {
      minerList.add("Miner" + (i+1));
    }
    
    return minerList;
  } 
}

class MinerThread extends Thread{
  public static boolean solutionClaimed = false;
  public static int claimerID = -1;
  public static int candidate = -1;
  public static ArrayList<Boolean> consensusList = new ArrayList<Boolean>();
  public static int minerNum = 0;
  private final int KEY = 123456;
  
  public int index;
  public int solution;
  
  public MinerThread(String minerID) {
    super(minerID);
    minerNum++;
    index = minerNum;
    consensusList.add(false);
    solution = 1;
    System.out.println("Creating miner thread: " + minerID);
  }
  
  @Override
  public void run() {
    System.out.println("Running miner thread: " + this.getName());
    while(!consensusAchieved()) {
      while(!solutionClaimed && solution % KEY != 0) {
        this.solution++;
      }
      if(solutionClaimed && candidate >= 0) {
        // if someone else claims that a solution is found, verify that
        if(candidate % KEY == 0) {
          consensusList.set(index-1, true);
        }
        else {
          // if this candidate fails the verification
          resetConsensus();
        }
        // TODO1: verify the claimed solution
        // TODO2: report your verification to the public
      } else if(this.solution % KEY == 0) {
        // if this miner finds a solution, report to the public, and wait for verification
        solutionClaimed = true;
        consensusList.set(index-1, true);
        candidate = solution;
        claimerID = index;
      }
    }
    System.out.println(this.getName() + " has approved that Miner" + claimerID + " came up with the correct solution: " + candidate);
    
  }
  
  private void resetConsensus() {
    // 1, reset the consensusList to all false
    for(int i = 0; i < consensusList.size(); i++) {
      consensusList.set(i, false);
    }
    // 2, reset candidate to -1
    candidate = -1;
    // 3, reset solutionClaimed to false
    solutionClaimed = false;
    // 4, reset claimerID to -1
    claimerID = -1;
  }
  
  private boolean consensusAchieved() {
    boolean agree = true;
    for(int i = 0; i < consensusList.size(); i++) {
      if( consensusList.get(i) == false ) {
        agree = false;
        break;
      }
    }
    return agree;
  }
}