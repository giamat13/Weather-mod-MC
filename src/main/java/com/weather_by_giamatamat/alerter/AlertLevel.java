package com.weather_by_giamatamat.alerter;

/**
 * The status light shown by an Alerter.
 * <ul>
 *   <li>{@link #GREEN}  – no relevant disaster nearby.</li>
 *   <li>{@link #YELLOW} – a relevant disaster within {@code ALERT_YELLOW_RANGE} blocks.</li>
 *   <li>{@link #RED}    – a relevant disaster within {@code ALERT_RED_RANGE} blocks (loud beeping).</li>
 * </ul>
 */
public enum AlertLevel {
	GREEN,
	YELLOW,
	RED
}
