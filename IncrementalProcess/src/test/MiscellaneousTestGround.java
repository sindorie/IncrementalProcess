package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import components.EventFactory;
import components.Executer;
import components.GraphicalLayout;
import components.system.Configuration;
import analysis.StaticInfo;
import staticFamily.StaticApp;
import support.ADBUtility;
import support.CommandLine;
import support.UIUtility;

public class MiscellaneousTestGround {

	public static void main(String[] args) {
		System.out.println(Configuration.getValue("sample"));
	}

}
