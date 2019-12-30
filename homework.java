import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class homework {

	public static void main(String[] args) {
		String fileName = "./input.txt";
		try {
			File file = new File(fileName);
			FileReader reader;

			reader = new FileReader(file);
			BufferedReader br = new BufferedReader(reader);
			String line;
			Queue<String> lines = new LinkedList<>();
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
			br.close();
			reader.close();
			long startTime = System.currentTimeMillis();

			ProblemStatement problemSpecs = processInput(lines);
			String[] solution = executeSearch(problemSpecs);
			FileWriter writer = new FileWriter(new File("./output.txt"));
			BufferedWriter bufferedWriter = new BufferedWriter(writer);
			if (solution.length > 1) {
				for (int idx = 0; idx < solution.length - 1; idx++) {
                    if(solution[idx] == "FAIL") bufferedWriter.write(solution[idx]);
					else bufferedWriter.write(solution[idx].substring(0, solution[idx].length()-1));
					bufferedWriter.newLine();
				}
			}
            if(solution[solution.length - 1] == "FAIL") bufferedWriter.write(solution[solution.length - 1]);
		    else bufferedWriter.write(solution[solution.length - 1].substring(0, solution[solution.length - 1].length()-1));
            
			bufferedWriter.close();
			writer.close();

			long endTime = System.currentTimeMillis();
			System.out.println("That took " + (endTime - startTime) + " milliseconds");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static ProblemStatement processInput(Queue<String> input) {
		String type = input.poll();
		String[] dimensions = input.poll().split("\\s+");
		int width = Integer.parseInt(dimensions[0]);
		int height = Integer.parseInt(dimensions[1]);
		String[] landingSite = input.poll().split("\\s+");
		int landingY = Integer.parseInt(landingSite[0]);
		int landingX = Integer.parseInt(landingSite[1]);
		int maxElevation = Integer.parseInt(input.poll());
		int numTargets = Integer.parseInt(input.poll());
		List<Coordinates> targets = new ArrayList<Coordinates>();
		HashMap<Coordinates, Coordinates> targetMap = new HashMap<>();
		for (int idx = 0; idx < numTargets; idx++) {
			String[] targetDimensions = input.poll().split("\\s+");
			Coordinates pt = new Coordinates(Integer.parseInt(targetDimensions[1]),
					Integer.parseInt(targetDimensions[0]));
			targets.add(pt);
			targetMap.put(pt, pt);
		}
		int idx = 0;
		int[][] map = new int[height][width];
		while (!input.isEmpty()) {
			String[] rowWidths = input.poll().split("\\s+");
			for (int Y = 0; Y < width; Y++) {
				map[idx][Y] = Integer.parseInt(rowWidths[Y]);
			}
			idx++;
		}
		return new ProblemStatement(type, width, height, landingX, landingY, maxElevation, numTargets, targets,
				targetMap, map);
	}

	public static String[] executeSearch(ProblemStatement problemStatement) {
		if (problemStatement.type.equalsIgnoreCase("bfs"))
			return applyBFS(problemStatement);
		if (problemStatement.type.equalsIgnoreCase("ucs"))
			return applyUCS(problemStatement);
		if (problemStatement.type.equalsIgnoreCase("a*"))
			return applyAStar(problemStatement);
		return null;
	}

	public static String[] applyBFS(ProblemStatement problemStatement) {
		int startRow = problemStatement.getLandingX();
		int startCol = problemStatement.getLandingY();
		int numTargets = problemStatement.getNumTargets();
		Coordinates startPoint = new Coordinates(startRow, startCol);
		HashMap<Coordinates, Coordinates> paths = new HashMap<Coordinates, Coordinates>();
		startPoint.parent = null;
		startPoint.pathCost = 0;

		HashMap<Coordinates, Coordinates> closedSet = new HashMap<Coordinates, Coordinates>();
		Queue<Coordinates> openSet = new LinkedList<Coordinates>();
		openSet.offer(startPoint);
		while (!openSet.isEmpty() && numTargets > 0) {
			Coordinates currentPt = openSet.poll();
			closedSet.put(currentPt, currentPt);
			if (problemStatement.targets.contains(currentPt)) {
				numTargets--;
				paths.put(problemStatement.targetMap.get(currentPt), currentPt);
			}
			List<Coordinates> neighbors = generateValidNeighborsForBFS(currentPt, problemStatement, closedSet);
			while (!neighbors.isEmpty()) {
				Coordinates neighbor = neighbors.remove(0);
				if (!openSet.contains(neighbor) && !closedSet.containsKey(neighbor)) {
					openSet.add(neighbor);
				}
			}
		}
		return printResult(problemStatement, paths);
	}

	public static String[] printResult(ProblemStatement ps, HashMap<Coordinates, Coordinates> paths) {
		String[] output = new String[ps.numTargets];
		int idx = 0;
		for (Coordinates target : ps.targets) {
			if (paths.containsKey(target)) {
				Coordinates node = paths.get(target);
				output[idx] = generatePath(node).concat(" Distance: "+node.pathCost+" ");
			} else
				output[idx] = "FAIL";
			idx++;
		}
		return output;
	}

	public static List<Coordinates> generateValidNeighborsForBFS(Coordinates currentPt, ProblemStatement ps,
			HashMap<Coordinates, Coordinates> closedSet) {
		List<Coordinates> neighbors = new ArrayList<Coordinates>();
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				if (currentPt.rowNo + i < 0 || currentPt.rowNo + i >= ps.height)
					continue;
				if (currentPt.colNo + j < 0 || currentPt.colNo + j >= ps.width)
					continue;
				if (i == 0 && j == 0)
					continue;
				int elevation = Math.abs(
						ps.map[currentPt.rowNo][currentPt.colNo] - ps.map[currentPt.rowNo + i][currentPt.colNo + j]);
				if (elevation > ps.maxElevation)
					continue;
				Coordinates neighbor = new Coordinates(currentPt.rowNo + i, currentPt.colNo + j, currentPt,
						currentPt.pathCost + 1);
				if (closedSet.containsKey(neighbor))
					continue;
				neighbors.add(neighbor);
			}
		}
		return neighbors;
	}

	public static String[] applyUCS(ProblemStatement problemStatement) {
		int startRow = problemStatement.getLandingX();
		int startCol = problemStatement.getLandingY();
		int numTargets = problemStatement.getNumTargets();
		Coordinates startPoint = new Coordinates(startRow, startCol);
		HashMap<Coordinates, Coordinates> paths = new HashMap<Coordinates, Coordinates>();
		startPoint.parent = null;
		startPoint.pathCost = 0;

		HashMap<Coordinates, Coordinates> closedSet = new HashMap<>();
		PriorityQueue<Coordinates> openSet = new PriorityQueue<Coordinates>(new Comparator<Coordinates>() {
			@Override
			public int compare(Coordinates p1, Coordinates p2) {
				if (p1.pathCost > p2.pathCost)
					return 1;
				if (p1.pathCost < p2.pathCost)
					return -1;
				return 0;
			}
		});
		HashMap<Coordinates, Coordinates> openSetMap = new HashMap<Coordinates, Coordinates>();
		openSet.offer(startPoint);
		openSetMap.put(startPoint, startPoint);
		while (!openSet.isEmpty() && numTargets > 0) {
			Coordinates currentPt = openSet.poll();
			openSetMap.remove(currentPt);
			closedSet.put(currentPt, currentPt);
			if (problemStatement.targets.contains(currentPt)) {
				if (!paths.containsKey(currentPt))
					numTargets--;
				paths.put(problemStatement.targetMap.get(currentPt), currentPt);
			}
			List<Coordinates> neighbors = generateValidNeighbors(currentPt, problemStatement, closedSet);
			while (!neighbors.isEmpty()) {
				Coordinates neighbor = neighbors.remove(0);
				if (openSet.contains(neighbor)) {
					Coordinates existingNeighbor = openSetMap.get(neighbor);
					if (existingNeighbor.pathCost > neighbor.pathCost) {
						existingNeighbor.parent = neighbor.parent;
						existingNeighbor.pathCost = neighbor.pathCost;
					}
				} else if (closedSet.containsKey(neighbor)) {
					if (closedSet.get(neighbor).pathCost > neighbor.pathCost) {
						closedSet.remove(neighbor);
						openSet.add(neighbor);
						openSetMap.put(neighbor, neighbor);
					}
				} else {
					openSet.add(neighbor);
					openSetMap.put(neighbor, neighbor);
				}
			}
		}
		return printResult(problemStatement, paths);
	}

	public static String generatePath(Coordinates goal) {
		if (goal == null)
			return "";
		else
			return generatePath(goal.parent).concat(goal.colNo + "," + goal.rowNo + " ");
	}

	public static List<Coordinates> generateValidNeighbors(Coordinates currentPt, ProblemStatement ps,
			HashMap<Coordinates, Coordinates> closedSet) {
		List<Coordinates> neighbors = new ArrayList<Coordinates>();
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				if (currentPt.rowNo + i < 0 || currentPt.rowNo + i >= ps.height)
					continue;
				if (currentPt.colNo + j < 0 || currentPt.colNo + j >= ps.width)
					continue;
				if (i == 0 && j == 0)
					continue;
				int elevation = Math.abs(
						ps.map[currentPt.rowNo][currentPt.colNo] - ps.map[currentPt.rowNo + i][currentPt.colNo + j]);
				if (elevation > ps.maxElevation)
					continue;
				Coordinates neighbor = new Coordinates(currentPt.rowNo + i, currentPt.colNo + j, currentPt, 0);
				if ((i == 1 && j == 1) || (i == -1 && j == 1) || (i == 1 && j == -1) || (i == -1 && j == -1))
					neighbor.pathCost = currentPt.pathCost + 14;
				else
					neighbor.pathCost = currentPt.pathCost + 10;

				neighbors.add(neighbor);
			}
		}
		return neighbors;
	}

	public static String[] applyAStar(ProblemStatement problemStatement) {
//		if ((problemStatement.height > 200 || problemStatement.width > 200) || problemStatement.numTargets > 30)
//			return applyAAStar(problemStatement);
		int startRow = problemStatement.getLandingX();
		int startCol = problemStatement.getLandingY();
		List<Coordinates> targets = new ArrayList<Coordinates>(problemStatement.targets);
		Coordinates startPoint = new Coordinates(startRow, startCol);
		HashMap<Coordinates, Coordinates> paths = new HashMap<Coordinates, Coordinates>();
		startPoint.parent = null;
		startPoint.pathCost = 0L;

		HashMap<Coordinates, Coordinates> closedSet = new HashMap<>();
		PriorityQueue<Coordinates> openSet = new PriorityQueue<Coordinates>(new Comparator<Coordinates>() {
			@Override
			public int compare(Coordinates p1, Coordinates p2) {
				if (p1.pathCost + p1.heuristicCost > p2.pathCost + p2.heuristicCost)
					return 1;
				if (p1.pathCost + p1.heuristicCost < p2.pathCost + p2.heuristicCost)
					return -1;
				return 0;
			}
		});
		HashMap<Coordinates, Coordinates> openSetMap = new HashMap<Coordinates, Coordinates>();
		while (!targets.isEmpty()) {
			Coordinates currentTarget = targets.remove(0);
			openSet.clear();
			openSetMap.clear();
			closedSet.clear();
			startPoint.heuristicCost = getHeuristicValue(startPoint, currentTarget, null);
			openSet.offer(startPoint);
			openSetMap.put(startPoint, startPoint);
			while (!openSet.isEmpty()) {
				Coordinates currentPt = openSet.poll();
				openSetMap.remove(currentPt);
				closedSet.put(currentPt, currentPt);
				if (currentTarget.equals(currentPt)) {
					paths.put(currentTarget, currentPt);
					break;
				}
				List<Coordinates> neighbors = generateValidNeighborsForAStar(currentPt, problemStatement,
						currentTarget);
				while (!neighbors.isEmpty()) {
					Coordinates neighbor = neighbors.remove(0);
					if (openSet.contains(neighbor)) {
						Coordinates existingNeighbor = openSetMap.get(neighbor);
						if (existingNeighbor.pathCost > neighbor.pathCost) {
							existingNeighbor.parent = neighbor.parent;
							existingNeighbor.pathCost = neighbor.pathCost;
						}
					} else if (closedSet.containsKey(neighbor)) {
						if (closedSet.get(neighbor).equals(neighbor)) {
							if (closedSet.get(neighbor).pathCost > neighbor.pathCost) {
								closedSet.remove(neighbor);
								openSet.add(neighbor);
								openSetMap.put(neighbor, neighbor);
							}
						}
					} else {
						openSet.add(neighbor);
						openSetMap.put(neighbor, neighbor);
					}
				}
			}
		}
		return printResult(problemStatement, paths);
	}

	public static List<Coordinates> generateValidNeighborsForAStar(Coordinates currentPt, ProblemStatement ps,
			Coordinates target) {
		List<Coordinates> neighbors = new ArrayList<Coordinates>();
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				if (currentPt.rowNo + i < 0 || currentPt.rowNo + i >= ps.height)
					continue;
				if (currentPt.colNo + j < 0 || currentPt.colNo + j >= ps.width)
					continue;
				if (i == 0 && j == 0)
					continue;
				int elevation = Math.abs(
						ps.map[currentPt.rowNo][currentPt.colNo] - ps.map[currentPt.rowNo + i][currentPt.colNo + j]);
				if (elevation > ps.maxElevation)
					continue;
				Coordinates neighbor = new Coordinates(currentPt.rowNo + i, currentPt.colNo + j, currentPt, 0);
				if ((i == 1 && j == 1) || (i == -1 && j == 1) || (i == 1 && j == -1) || (i == -1 && j == -1))
					neighbor.pathCost = currentPt.pathCost + 14L;
				else
					neighbor.pathCost = currentPt.pathCost + 10L;

				neighbor.pathCost += (long) elevation;
				neighbor.heuristicCost = getHeuristicValue(neighbor, target, currentPt);
				neighbors.add(neighbor);
			}
		}
		return neighbors;
	}

	public static long getHeuristicValue(Coordinates pt, Coordinates target, Coordinates parent) {
		long cost = Math.abs(pt.rowNo - target.rowNo) + Math.abs(pt.colNo - target.colNo);
		double offset = 0.01 * cost;
		if (parent != null && pt.rowNo != parent.rowNo && pt.colNo != parent.colNo)
			cost = Math.max(cost - 4L - (long) offset, 0);
		return cost;
	}

	public static String[] applyAAStar(ProblemStatement problemStatement) {
		int startRow = problemStatement.getLandingX();
		int startCol = problemStatement.getLandingY();
		int numTargets = problemStatement.getNumTargets();
		Coordinates startPoint = new Coordinates(startRow, startCol);
		HashMap<Coordinates, Coordinates> paths = new HashMap<Coordinates, Coordinates>();
		startPoint.parent = null;
		startPoint.pathCost = 0;

		HashMap<Coordinates, Coordinates> closedSet = new HashMap<>();
		PriorityQueue<Coordinates> openSet = new PriorityQueue<Coordinates>(new Comparator<Coordinates>() {
			@Override
			public int compare(Coordinates p1, Coordinates p2) {
				if (p1.pathCost > p2.pathCost)
					return 1;
				if (p1.pathCost < p2.pathCost)
					return -1;
				return 0;
			}
		});
		HashMap<Coordinates, Coordinates> openSetMap = new HashMap<Coordinates, Coordinates>();
		openSet.offer(startPoint);
		openSetMap.put(startPoint, startPoint);
		while (!openSet.isEmpty() && numTargets > 0) {
			Coordinates currentPt = openSet.poll();
			openSetMap.remove(currentPt);
			closedSet.put(currentPt, currentPt);
			if (problemStatement.targets.contains(currentPt)) {
				if (!paths.containsKey(currentPt))
					numTargets--;
				paths.put(problemStatement.targetMap.get(currentPt), currentPt);
			}
			List<Coordinates> neighbors = generateValidNeighborsForAAStar(currentPt, problemStatement, closedSet);
			while (!neighbors.isEmpty()) {
				Coordinates neighbor = neighbors.remove(0);
				if (openSet.contains(neighbor)) {
					Coordinates existingNeighbor = openSetMap.get(neighbor);
					if (existingNeighbor.pathCost > neighbor.pathCost) {
						existingNeighbor.parent = neighbor.parent;
						existingNeighbor.pathCost = neighbor.pathCost;
					}
				} else if (closedSet.containsKey(neighbor)) {
					if (closedSet.get(neighbor).pathCost > neighbor.pathCost) {
						closedSet.remove(neighbor);
						openSet.add(neighbor);
						openSetMap.put(neighbor, neighbor);
					}
				} else {
					openSet.add(neighbor);
					openSetMap.put(neighbor, neighbor);
				}
			}
		}
		return printResult(problemStatement, paths);
	}

	public static List<Coordinates> generateValidNeighborsForAAStar(Coordinates currentPt, ProblemStatement ps,
			HashMap<Coordinates, Coordinates> closedSet) {
		List<Coordinates> neighbors = new ArrayList<Coordinates>();
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				if (currentPt.rowNo + i < 0 || currentPt.rowNo + i >= ps.height)
					continue;
				if (currentPt.colNo + j < 0 || currentPt.colNo + j >= ps.width)
					continue;
				if (i == 0 && j == 0)
					continue;
				int elevation = Math.abs(
						ps.map[currentPt.rowNo][currentPt.colNo] - ps.map[currentPt.rowNo + i][currentPt.colNo + j]);
				if (elevation > ps.maxElevation)
					continue;
				Coordinates neighbor = new Coordinates(currentPt.rowNo + i, currentPt.colNo + j, currentPt, 0);
				if ((i == 1 && j == 1) || (i == -1 && j == 1) || (i == 1 && j == -1) || (i == -1 && j == -1))
					neighbor.pathCost = currentPt.pathCost + 14;
				else
					neighbor.pathCost = currentPt.pathCost + 10;

				neighbor.pathCost = neighbor.pathCost + (long) elevation;
				neighbors.add(neighbor);
			}
		}
		return neighbors;
	}
}

class ProblemStatement {
	String type;
	int width;
	int height;
	int landingX;
	int landingY;
	int maxElevation;
	int numTargets;
	List<Coordinates> targets;
	HashMap<Coordinates, Coordinates> targetMap;
	int[][] map;

	public ProblemStatement(String type, int width, int height, int landingX, int landingY, int maxElevation,
			int numTargets, List<Coordinates> targets, HashMap<Coordinates, Coordinates> targetMap, int[][] map) {
		super();
		this.type = type;
		this.width = width;
		this.height = height;
		this.landingX = landingX;
		this.landingY = landingY;
		this.maxElevation = maxElevation;
		this.numTargets = numTargets;
		this.targets = targets;
		this.targetMap = targetMap;
		this.map = map;
	}

	protected String getType() {
		return type;
	}

	protected void setType(String type) {
		this.type = type;
	}

	protected int getWidth() {
		return width;
	}

	protected void setWidth(int width) {
		this.width = width;
	}

	protected int getHeight() {
		return height;
	}

	protected void setHeight(int height) {
		this.height = height;
	}

	protected int getLandingX() {
		return landingX;
	}

	protected void setLandingX(int landingX) {
		this.landingX = landingX;
	}

	protected int getLandingY() {
		return landingY;
	}

	protected void setLandingY(int landingY) {
		this.landingY = landingY;
	}

	protected int getMaxElevation() {
		return maxElevation;
	}

	protected void setMaxElevation(int maxElevation) {
		this.maxElevation = maxElevation;
	}

	protected int getNumTargets() {
		return numTargets;
	}

	protected void setNumTargets(int numTargets) {
		this.numTargets = numTargets;
	}

	protected List<Coordinates> getTargets() {
		return targets;
	}

	protected void setTargets(List<Coordinates> targets) {
		this.targets = targets;
	}

	protected int[][] getMap() {
		return map;
	}

	protected void setMap(int[][] map) {
		this.map = map;
	}

}

class Coordinates {
	int rowNo;
	int colNo;
	long pathCost;
	long heuristicCost;
	Coordinates parent;

	public Coordinates(int row, int col) {
		this.rowNo = row;
		this.colNo = col;
	}

	public Coordinates(int row, int col, Coordinates parent, long pathCost) {
		this.rowNo = row;
		this.colNo = col;
		this.parent = parent;
		this.pathCost = pathCost;
	}

	public Coordinates(Coordinates point) {
		this.rowNo = point.rowNo;
		this.colNo = point.colNo;
		this.parent = point.parent;
		this.pathCost = point.pathCost;
		this.heuristicCost = point.heuristicCost;
	}

	@Override
	public boolean equals(Object v) {
		boolean retVal = false;

		if (v instanceof Coordinates) {
			Coordinates ptr = (Coordinates) v;
			retVal = ptr.colNo == this.colNo && ptr.rowNo == this.rowNo;
		}
		return retVal;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * this.rowNo + 7 * this.colNo;
		return hash;
	}
}