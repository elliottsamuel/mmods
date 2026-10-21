package com.findyourpath.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

import com.findyourpath.client.Quests.Role;
import com.findyourpath.client.Quests.Step;

/** Builds each screen and moves between them. */
public final class Flow {
	private Flow() {}

	private static void open(TabletScreen s) { Minecraft.getInstance().gui.setScreen(s); }
	private static void close() { Minecraft.getInstance().gui.setScreen(null); }

	/** What G opens: always the welcome screen. */
	public static void welcome() {
		open(new TabletScreen()
				.heading(Items.COMPASS, "Welcome to Find Your Path", Ui.GOLD)
				.para("A guide for new players: pick a play style and learn Minecraft one small step at a time.")
				.button("▶ Continue", Flow::afterWelcome)
				.button("Close", Flow::close).columns(2));
	}

	private static void afterWelcome() {
		Role r = FindYourPathClient.currentRole();
		if (r == null) { menu(); return; }
		open(new TabletScreen()
				.header("Find Your Path", r.color())
				.heading(r.icon(), "You're on the " + r.name() + " quest", r.color())
				.para("Step " + (FindYourPathClient.currentStepIndex() + 1) + " of " + r.steps().size()
						+ ". Pick up where you left off, or abandon it and choose a new path.")
				.button("▶ Continue my quest", Flow::currentStep)
				.button("Abandon quest", () -> { FindYourPathClient.abandon(); menu(); }));
	}

	public static void menu() {
		TabletScreen s = new TabletScreen()
				.heading(Items.COMPASS, "Choose your path", Ui.GOLD)
				.para("Minecraft has no single goal. You choose what kind of fun to have. Tap a path to learn what it is. You can switch anytime.");
		for (Role r : Quests.ROLES) s.button(r.name(), () -> about(r));
		s.button("Close", Flow::close);
		open(s);
	}

	public static void about(Role r) {
		int i = Quests.ROLES.indexOf(r);
		Role next = Quests.ROLES.get((i + 1) % Quests.ROLES.size());
		TabletScreen s = new TabletScreen()
				.header("The " + r.name(), r.color())
				.heading(r.icon(), r.tagline(), r.color());
		for (String p : r.about()) s.para(p);
		s.button("▶ Start as " + r.name(), () -> brief(r))
				.button("Next path: " + next.name() + " →", () -> about(next))
				.button("Back to all paths", Flow::menu);
		open(s);
	}

	public static void brief(Role r) {
		FindYourPathClient.start(r);
		open(new TabletScreen()
				.header(r.name() + " Quest", r.color())
				.heading(r.icon(), r.goal(), r.color())
				.para(r.steps().size() + " small steps, about 5 minutes. After each step, this tablet comes back with the next one. "
						+ "Your current step also shows above your hotbar, and next to the crafting grid.")
				.button("Show me step 1", Flow::currentStep));
	}

	public static void currentStep() {
		Role r = FindYourPathClient.currentRole();
		if (r == null) { menu(); return; }
		int i = FindYourPathClient.currentStepIndex();
		Step st = r.steps().get(i);
		TabletScreen s = new TabletScreen()
				.header(r.name() + " · Step " + (i + 1) + " of " + r.steps().size(), r.color())
				.progress(i + 1, r.steps().size())
				.heading(st.icon(), st.title(), r.color())
				.para(st.howTo(), Ui.WHITE)
				.para(st.tip(), Ui.MUTED);
		if (st.recipe() != null) s.recipe(st.recipe(), st.icon());
		s.button("Got it!", () -> { close(); FindYourPathClient.onStepScreenClosed(); })
				.button("Main menu", Flow::menu).columns(2);
		s.onClose(FindYourPathClient::onStepScreenClosed);
		open(s);
	}

	public static void complete(Role r) {
		TabletScreen s = new TabletScreen()
				.header("Quest complete!", Ui.GOLD)
				.heading(r.icon(), "You tried being a " + r.name() + "!", r.color())
				.para("How did that feel? There's no wrong answer. The point is to find the kind of fun that's yours.")
				.para("Ideas for what " + r.name() + "s do next:", Ui.WHITE);
		for (String n : r.next()) s.para("• " + n);
		s.button("Try another path", Flow::menu).button("Just let me play", Flow::close).columns(2);
		open(s);
	}
}
