/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shootoff.session.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.shootoff.camera.Shot;
import com.shootoff.session.Event;
import com.shootoff.session.ExerciseFeedMessageEvent;
import com.shootoff.session.ShotEvent;
import com.shootoff.session.TargetAddedEvent;
import com.shootoff.session.TargetMovedEvent;
import com.shootoff.session.TargetRemovedEvent;
import com.shootoff.session.TargetResizedEvent;

import javafx.scene.paint.Color;

public class XMLSessionReader {
	private final File sessionFile;
	
	private long lastTimestamp;
	private boolean exerciseFeedMessage = false;
	
	public XMLSessionReader(File sessionFile) {
		this.sessionFile = sessionFile;
	}
	
	public Map<String, List<Event>> load() {
		InputStream xmlInput = null;
		try {
			xmlInput = new FileInputStream(sessionFile);
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			SessionXMLHandler handler = new SessionXMLHandler();
			saxParser.parse(xmlInput, handler);
			
			return handler.getEvents();
		} catch (IOException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		} finally {
			if (xmlInput != null) {
				try {
					xmlInput.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return new HashMap<String, List<Event>>();
	}
	
	private class SessionXMLHandler extends DefaultHandler {
		private final Map<String, List<Event>> events = new HashMap<String, List<Event>>();
		private String currentCameraName = "";
		
		public Map<String, List<Event>> getEvents() {
			return events;
		}
		
		public void startElement(String uri, String localName, String qName, 
                Attributes attributes) throws SAXException {
			
			switch (qName) {
			case "camera":
				currentCameraName = attributes.getValue("name");
				events.put(currentCameraName, new ArrayList<Event>());
				break;
			
			case "shot":
				Color c;
				
				if (attributes.getValue("color").equals("0xff0000ff")) {
					c = Color.RED;
				} else {
					c = Color.GREEN;
				}
				
				Shot shot = new Shot(c, Double.parseDouble(attributes.getValue("x")), 
						Double.parseDouble(attributes.getValue("y")), Long.parseLong(attributes.getValue("shotTimestamp")), 
						Integer.parseInt(attributes.getValue("markerRadius")));
				
				boolean isMalfunction = Boolean.parseBoolean(attributes.getValue("isMalfunction"));
				
				boolean isReload = Boolean.parseBoolean(attributes.getValue("isReload"));
				
				Optional<Integer> targetIndex;
				int index = Integer.parseInt(attributes.getValue("targetIndex")); 
				if (index == -1) {
					targetIndex = Optional.empty();
				} else {
					targetIndex = Optional.of(index);
				}
				
				Optional<Integer> hitRegionIndex;
				index = Integer.parseInt(attributes.getValue("hitRegionIndex")); 
				if (index == -1) {
					hitRegionIndex = Optional.empty();
				} else {
					hitRegionIndex = Optional.of(index);
				}
				
				Optional<String> videoString = Optional.ofNullable(attributes.getValue("videos"));
				
				events.get(currentCameraName).add(
						new ShotEvent(currentCameraName, Long.parseLong(attributes.getValue("timestamp")), shot, 
								isMalfunction, isReload, targetIndex, hitRegionIndex, videoString));
				
				break;

			case "targetAdded":
				events.get(currentCameraName).add(
						new TargetAddedEvent(currentCameraName, Long.parseLong(attributes.getValue("timestamp")), 
								attributes.getValue("name")));
				
				break;
				
			case "targetRemoved":
				events.get(currentCameraName).add(
						new TargetRemovedEvent(currentCameraName, Long.parseLong(attributes.getValue("timestamp")), 
								Integer.parseInt(attributes.getValue("index"))));
				
				break;
				
			case "targetResized":
				events.get(currentCameraName).add(
						new TargetResizedEvent(currentCameraName, Long.parseLong(attributes.getValue("timestamp")), 
								Integer.parseInt(attributes.getValue("index")), 
								Double.parseDouble(attributes.getValue("newWidth")),
								Double.parseDouble(attributes.getValue("newHeight"))));
				
				break;
				
			case "targetMoved":
				events.get(currentCameraName).add(
						new TargetMovedEvent(currentCameraName, Long.parseLong(attributes.getValue("timestamp")), 
								Integer.parseInt(attributes.getValue("index")), 
								Integer.parseInt(attributes.getValue("newX")),
								Integer.parseInt(attributes.getValue("newY"))));
				break;
				
			case "exerciseFeedMessage":
				lastTimestamp = Long.parseLong(attributes.getValue("timestamp"));
				exerciseFeedMessage = true;
			}
		}
		
		public void characters(char ch[], int start, int length) throws SAXException {
			if (exerciseFeedMessage) {
				events.get(currentCameraName).add(
						new ExerciseFeedMessageEvent(currentCameraName, lastTimestamp, 
								new String(ch, start, length)));
				
				exerciseFeedMessage = false;
			}	
		}
	}
}
