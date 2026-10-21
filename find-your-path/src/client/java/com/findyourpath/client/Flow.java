package com.findyourpath.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

import com.findyourpath.client.Quests.Role;
import com.findyourpath.client.Quests.Step;

/** Builds each screen and moves between them. */
public final class Flow {
	private Flow() {}

	private static final int LOCKED = 0xFF5A6380;

	private static void open(TabletScreen s) { Minecraft.getInstance().gui.setScreen(s); }
	private static void close() { Minecraft.getInstance().gui.setScreen(null); }

	/** What G opens: always the welcome screen. */
	public static void welcome() {
		open(new TabletScreen()
				.heading(Items.COMPASS, "Welcome to Find Your Path", Ui.WHITE)
				.para("A guide for new players: learn the basics, then find the kind of player you are, one small step at a time.")
				.button("▶ Continue", Flow::afterWelcome)
				.button("Close", Flow::close).columns(2));
	}

	private static void afterWelcome() {
		Role r = FindYourPathClient.currentRole();
		if (r == null) { menu(); return; }
		open(new TabletScreen()
				.header("Find Your Path", r.color())
				.heading(r.icon(), "You're on: " + r.name(), r.color())
				.para("Step " + (FindYourPathClient.currentStepIndex() + 1) + " of " + r.steps().size()
						+ ". Pick up where you left off, or abandon it and choose something else.")
				.button("▶ Continue", Flow::currentStep)
				.button("Abandon", () -> { FindYourPathClient.abandon(); menu(); }));
	}

	public static void menu() {
		boolean unlocked = FindYourPathClient.level1Done();
		TabletScreen s = new TabletScreen().header("Find Your Path", Ui.CYAN);
		if (!unlocked) {
			s.para("Start with Level 1 to learn the basics. Finishing it unlocks the five paths.");
		} else {
			s.para("Level 1 complete! Pick a path to find the kind of fun that's yours, or browse tips.");
		}
		Role l1 = Quests.FIRST_DAY;
		s.tile(unlocked ? "First Day ✔" : "Level 1", l1.icon(), l1.color(), () -> about(l1));
		for (Role r : Quests.ROLES) {
			if (unlocked) s.tile(r.name(), r.icon(), r.color(), () -> about(r));
			else s.tile("🔒 " + r.name(), r.icon(), LOCKED, () -> about(r));
		}
		s.tile("Tips", Items.GLOWSTONE_DUST, Ui.LIME, () -> tips(0));
		s.button("Close", Flow::close);
		open(s);
	}

	/** Browse all the tips, one per page, each with a little picture. */
	public static void tips(int index) {
		int n = Quests.TIPS.size();
		int i = ((index % n) + n) % n;
		Quests.Tip t = Quests.TIPS.get(i);
		open(new TabletScreen()
				.header("Tips  ·  " + (i + 1) + " of " + n, Ui.LIME)
				.progress(i + 1, n)
				.heading(t.icon(), t.title(), Ui.WHITE)
				.para(t.text(), Ui.SOFT)
				.picture(t.picture())
				.button("◀ Prev", () -> tips(i - 1))
				.button("Menu", Flow::menu)
				.button("Next ▶", () -> tips(i + 1))
				.columns(3));
	}

	public static void about(Role r) {
		boolean isL1 = r == Quests.FIRST_DAY;
		boolean locked = !isL1 && !FindYourPathClient.level1Done();
		TabletScreen s = new TabletScreen()
				.header(isL1 ? "Level 1" : "Level 2  ·  The " + r.name(), locked ? LOCKED : r.color())
				.heading(r.icon(), isL1 ? r.name() : r.tagline(), locked ? Ui.MUTED : r.color());
		for (String p : r.about()) s.para(p);
		if (locked) {
			s.para("🔒 Finish Level 1, Survive Your First Day, to unlock this path.", Ui.GOLD);
			s.button("▶ Go to Level 1", () -> about(Quests.FIRST_DAY));
		} else {
			s.button("▶ Start" + (isL1 ? "" : " as " + r.name()), () -> brief(r));
		}
		if (!isL1) {
			int i = Quests.ROLES.indexOf(r);
			Role next = Quests.ROLES.get((i + 1) % Quests.ROLES.size());
			s.button("Next path: " + next.name() + " →", () -> about(next));
		}
		s.button("Back", Flow::menu);
		open(s);
	}

	public static void brief(Role r) {
		FindYourPathClient.start(r);
		open(new TabletScreen()
				.header(r == Quests.FIRST_DAY ? "Level 1" : r.name() + " Quest", r.color())
				.heading(r.icon(), r.goal(), r.color())
				.para(r.steps().size() + " small steps, about " + r.minutes() + " minutes.")
				.button("Show me step 1", Flow::currentStep));
	}

	public static void currentStep() {
		Role r = FindYourPathClient.currentRole();
		if (r == null) { menu(); return; }
		int i = FindYourPathClient.currentStepIndex();
		Step st = r.steps().get(i);
		boolean side = Quests.isSideLesson(r);
		boolean hideHow = st.recall() && !FindYourPathClient.hintShown();

		TabletScreen s = new TabletScreen()
				.header((side ? "Side lesson: " : "") + r.name() + "  ·  Step " + (i + 1) + " of " + r.steps().size(), r.color())
				.progress(i + 1, r.steps().size())
				.heading(st.icon(), st.title(), r.color());
		if (hideHow) {
			s.para("You learned this in Level 1. Try it from memory!", Ui.WHITE);
			s.para("Stuck? Tap \"Show me how\".", Ui.MUTED);
		} else {
			s.para(st.howTo(), Ui.WHITE).para(st.tip(), Ui.MUTED);
			if (st.recipe() != null) s.recipe(st.recipe(), st.icon());
		}

		// waiting for night: offer side lessons that fit in the time left
		Tracker t = FindYourPathClient.tracker();
		if (st == Quests.SHELTER && t.minutesToNight() > 2) {
			int m = t.minutesToNight();
			s.para("Night falls in about " + m + " minutes. Want to learn something while you wait?", Ui.GOLD);
			for (Role lesson : Quests.SIDE_LESSONS) {
				if (lesson.minutes() < m) {
					s.tile(lesson.name() + " · " + lesson.minutes() + "m", lesson.icon(), lesson.color(), () -> {
						FindYourPathClient.startSideLesson(lesson);
						currentStep();
					});
				}
			}
		}

		s.button("Got it!", () -> { close(); FindYourPathClient.onStepScreenClosed(); });
		if (hideHow) s.button("Show me how", () -> { FindYourPathClient.showHint(); currentStep(); });
		if (st.skippable()) s.button("Skip this step", () -> { FindYourPathClient.skipStep(); });
		s.button("Menu", Flow::menu);
		s.columns(2);
		s.onClose(FindYourPathClient::onStepScreenClosed);
		open(s);
	}

	/** After a side lesson: back to the main quest. */
	public static void sideLessonDone(Role lesson) {
		Tracker t = FindYourPathClient.tracker();
		int m = t.minutesToNight();
		open(new TabletScreen()
				.header("Side lesson complete", Ui.GREEN)
				.heading(lesson.icon(), "Nice! You learned: " + lesson.name(), Ui.WHITE)
				.para(m > 0 ? "Night falls in about " + m + " minutes. Back to your first day." : "It's getting dark! Back to your first day.")
				.button("▶ Back to my quest", Flow::currentStep));
	}

	/** Shown once, the first time you die. */
	public static void firstDeath() {
		open(new TabletScreen()
				.header("You died", 0xFFFF5A6E)
				.heading(Items.CHEST, "When you die, you drop everything you were carrying.", Ui.WHITE)
				.para("Your items are lying on the ground where you died. Go back within about 5 minutes to pick them up, or they disappear for good.")
				.para("If you lost things you needed for your quest, like logs, you'll just need to collect them again.", Ui.MUTED)
				.button("Got it", Flow::close));
	}

	public static void complete(Role r) {
		if (r == Quests.FIRST_DAY) {
			open(new TabletScreen()
					.header("Level 1 complete!", Ui.GREEN)
					.heading(Items.TORCH, "You survived your first day!", Ui.WHITE)
					.para("You know the basics now: wood, tools, light, food and shelter. The five paths are unlocked.")
					.para("Each path builds on what you learned. Some steps will ask you to remember recipes from Level 1.", Ui.MUTED)
					.button("▶ Choose your path", Flow::menu)
					.button("Just let me play", Flow::close).columns(2));
			return;
		}
		TabletScreen s = new TabletScreen()
				.header("Quest complete!", Ui.GREEN)
				.heading(r.icon(), "You tried being a " + r.name() + "!", r.color())
				.para("How did that feel? There's no wrong answer. The point is to find the kind of fun that's yours.")
				.para("Ideas for what " + r.name() + "s do next:", Ui.WHITE);
		for (String n : r.next()) s.para("• " + n);
		s.button("Try another path", Flow::menu).button("Just let me play", Flow::close).columns(2);
		open(s);
	}
}
