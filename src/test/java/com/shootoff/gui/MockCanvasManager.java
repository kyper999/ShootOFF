package com.shootoff.gui;

import java.util.ArrayList;
import java.util.List;

import com.shootoff.camera.Shot;
import com.shootoff.config.Configuration;

import javafx.scene.canvas.Canvas;

public class MockCanvasManager extends CanvasManager {
	private final List<Shot> shots = new ArrayList<Shot>();
	
	public MockCanvasManager(Configuration config) {
		super(new Canvas(), config);
	}

	@Override
	public void addShot(Shot shot) {
		shots.add(shot);
	}
	
	public List<Shot> getShots() {
		return shots;
	}
}
