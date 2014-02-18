package com.github.lunatrius.ingameinfo;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;

import java.util.EnumSet;

public class Ticker implements ITickHandler {
	public static boolean enabled = true;
	public static boolean showInChat = true;

	private EnumSet<TickType> ticks = EnumSet.noneOf(TickType.class);
	private final Minecraft client;
	private final InGameInfoCore core;

	public Ticker(EnumSet<TickType> tickTypes, InGameInfoCore core) {
		this.ticks = tickTypes;
		this.client = Minecraft.getMinecraft();
		this.core = core;
	}

	@Override
	public void tickStart(EnumSet<TickType> tickTypes, Object... tickData) {
		onTick(tickTypes, true);
	}

	@Override
	public void tickEnd(EnumSet<TickType> tickTypes, Object... tickData) {
		onTick(tickTypes, false);
	}

	private void onTick(EnumSet<TickType> tickTypes, boolean start) {
		if (!start) {
			for (TickType tickType : tickTypes) {
				if (enabled && this.client != null && this.client.gameSettings != null && !this.client.gameSettings.showDebugInfo) {
					if (this.client.currentScreen == null || showInChat && this.client.currentScreen instanceof GuiChat) {
						if (tickType == TickType.CLIENT) {
							this.core.onTickClient();
						} else if (tickType == TickType.RENDER) {
							this.core.onTickRender();
						}
					}
				} else {
					this.core.reset();
				}
			}
		}
	}

	@Override
	public EnumSet<TickType> ticks() {
		return this.ticks;
	}

	@Override
	public String getLabel() {
		return "InGameInfoXMLTicker";
	}
}
