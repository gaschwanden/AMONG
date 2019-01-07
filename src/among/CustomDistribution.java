package among;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

/**
 * Class to provide real-world data to the simulation Provides randomly drawn
 * household income, assets and property value data
 * 
 * @author Zita Ultmann, The University of Melbourne
 *
 */
public class CustomDistribution {

	private List<String> lines;
	private String fileName;

	private double sumWeight;
	private Double[] weights;
	private Integer[] firstArray;
	private int indexfirstArray;

	CustomDistribution(String s) {
		if (s.equals("property")) {
			fileName = "filtered_property.csv"; // structure: property value, weight
		}
		if (s.contains("income")) {
			fileName = "filteredHH_ABS.csv"; // structure: yearly income, weight, wealth
		}

		try {
			lines = Files.readAllLines(Paths.get("src/among/data/" + fileName));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		// TODO
		if (s.equals("incomeShock")) {

		}
		if (!s.equals("incomeShock")) {
			weights = new Double[lines.get(1).split(",").length];
			firstArray = new Integer[lines.get(0).split(",").length];
			for (int i = 0; i < lines.get(0).split(",").length; i++) {
				weights[i] = Double.valueOf(lines.get(1).split(",")[i]);
				firstArray[i] = (int) Math.round(Double.valueOf(lines.get(0).split(",")[i]));
				sumWeight += weights[i];
			}
		}
	}

	// random sample for propertyValue and income;
	// for reference:
	// http://www.stat.cmu.edu/~cshalizi/350/lectures/28/lecture-28.pdf
	Integer Sample1(Random rnd) {
		double sum = 0;
		double dd = rnd.nextDouble();
		for (int i = 0; i < weights.length; i++) {
			sum += weights[i];
			if (dd < sum / sumWeight) {
				indexfirstArray = i;
				return firstArray[i];
			}
		}
		return null;
	}

	// income dependent wealth value
	// note: you have to use Sample1() first
	Integer Sample2(Random rnd) {
		String[] temp0;
		int temp = 0;
		int temp1 = indexfirstArray;
		if (lines.size() > 2) {
			temp0 = lines.get(2).split(",")[temp1].split(";");
			temp = (int) Math.round(Double.valueOf(temp0[rnd.nextInt(temp0.length)]));
		} else {
			System.out.println("Only one data-set in file");
		}
		return temp;
	}

}