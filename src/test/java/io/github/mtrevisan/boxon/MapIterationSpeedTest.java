package io.github.mtrevisan.boxon;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;


class MapIterationSpeedTest{

	public static void main(String[] args) {
		// Creazione di una mappa con un gran numero di elementi
		Map<String, Object> context = generateTestData(1000000);

		// Test del metodo tradizionale
		long startTime = System.currentTimeMillis();
		iterateTraditionally(context);
		long traditionalTime = System.currentTimeMillis() - startTime;
		System.out.println("Tempo impiegato per l'iterazione tradizionale: " + traditionalTime + " ms");

		// Test del metodo parallelo
		startTime = System.currentTimeMillis();
		iterateInParallel(context);
		long parallelTime = System.currentTimeMillis() - startTime;
		System.out.println("Tempo impiegato per l'iterazione parallela: " + parallelTime + " ms");
	}

	// Genera una mappa di test con un numero specifico di elementi
	private static Map<String, Object> generateTestData(int size) {
		Map<String, Object> testData = new HashMap<>();
		Random random = new Random();
		for (int i = 0; i < size; i++) {
			testData.put("key" + i, random.nextInt());
		}
		return testData;
	}

	// Iterazione tradizionale sulla mappa
	private static void iterateTraditionally(Map<String, Object> context) {
		for (Map.Entry<String, Object> entry : context.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			// Operazioni da eseguire su ogni entry
		}
	}

	// Iterazione parallela sulla mappa
	private static void iterateInParallel(Map<String, Object> context) {
		context.entrySet().forEach(entry -> {
			String key = entry.getKey();
			Object value = entry.getValue();
			// Operazioni da eseguire su ogni entry
		});
	}

}
