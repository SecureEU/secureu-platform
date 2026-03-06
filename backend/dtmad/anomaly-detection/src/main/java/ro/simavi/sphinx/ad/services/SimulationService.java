package ro.simavi.sphinx.ad.services;

import ro.simavi.sphinx.ad.model.simulation.AlgorithmSimulationModel;

import java.util.List;

public interface SimulationService {

    List<AlgorithmSimulationModel> getAlgorithmSimulationList();

    Integer execute(String file);
}
