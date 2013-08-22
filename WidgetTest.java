/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.algol.widgettest;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;
import org.powerbot.event.PaintListener;
import org.powerbot.script.AbstractScript;
import org.powerbot.script.Manifest;
import org.powerbot.script.wrappers.Widget;

/**
 *
 * @author Algol
 */
@Manifest(authors = "Algol", name = "WidgetTest", description = "Algol Widget Test")
public class WidgetTest extends AbstractScript implements PaintListener {

    @Override
    public void run() {

        final WidgetSearch widgetSearch = new WidgetSearch();

        final long initTime = System.currentTimeMillis();
        widgetSearch.init();
        System.out.println("Init Took: " + (System.currentTimeMillis() - initTime));

        final long searchTime = System.currentTimeMillis();
        ArrayList<SearchData> searchResult = widgetSearch.search("mini");
        System.out.println("Search Took: " + (System.currentTimeMillis() - searchTime));

        for (SearchData searchData : searchResult) {
            System.out.println(searchData.toString());
        }

        while (!getController()
                .isStopping()) {
            ctx.game.sleep(500);
        }
    }

    @Override
    public void repaint(Graphics grphcs) {
        final Graphics2D g2d = (Graphics2D) grphcs;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setColor(Color.CYAN);
        final Point mp = ctx.mouse.getLocation();
        final Dimension d = ctx.game.getDimensions();
        g2d.drawLine(0, mp.y, d.width, mp.y);
        g2d.drawLine(mp.x, 0, mp.x, d.height);
    }

    /**
     * AWidgetData class to hold Widget index and a list of child Components
     */
    final class AWidgetData {

        final int widgetIndex;
        final ArrayList<AComponentData> childComponents = new ArrayList<>();

        public AWidgetData(final int widgetIndex) {
            this.widgetIndex = widgetIndex;
        }
    }

    /**
     * AComponentData class to hold the Component + list of child Components, if
     * any and the depth in the Component hierarchy
     */
    final class AComponentData {

        public final org.powerbot.script.wrappers.Component comp;
        public ArrayList<AComponentData> childComponents = null;
        public int depth = 0;

        public AComponentData(final org.powerbot.script.wrappers.Component comp) {
            this.comp = comp;
        }

        /**
         *
         * @param s Search string
         * @return true if any of the Components String types contains String s
         */
        final public boolean hasString(final String s) {
            if (comp.getText().toLowerCase().contains(s)) {
                return true;
            }
            if (comp.getItemName().toLowerCase().contains(s)) {
                return true;
            }
            if (comp.getSelectedAction().toLowerCase().contains(s)) {
                return true;
            }
            for (String sa : comp.getActions()) {
                if ((sa == null ? false : sa.toLowerCase().contains(s))) {
                    return true;
                }
            }
            if (comp.getTooltip().toLowerCase().contains(s)) {
                return true;
            }
            return false;
        }
    }

    /**
     * SearchData class to hold info about a Component
     */
    final class SearchData {

        final int widgetIndex;
        final int componentIndex;
        final int parentCompIndex;

        public SearchData(final int widgetIndex, final int componentIndex, final int parentCompIndex) {
            this.widgetIndex = widgetIndex;
            this.componentIndex = componentIndex;
            this.parentCompIndex = parentCompIndex;
        }

        /**
         * 
         * @return String representation of the path to this Component
         * return format:
         * #get(WidgetIndex, ComponentIndex) Or
         * #get(WidgetIndex, ParentComponentIndex).get(ComponentIndex)
         * 
         */
        @Override
        final public String toString() {
            final String mid = (parentCompIndex == -1 ? ", " : ", " + parentCompIndex + ").get(");
            final String end = mid + componentIndex + ")";
            return ".get(" + widgetIndex + end;
        }
    }

    /**
     * WidgetSearch class, which holds the loaded and valid Widgets and their Components
     * and provides a method to search for a string, in the loaded Components.
     */
    final class WidgetSearch {

        private ArrayList<AWidgetData> widgetList = new ArrayList<>();
        private Widget[] widgets;

        /**
         * Initializes this class with the loaded and valid Widgets and their Components.
         * Each valid Widget is added to a Widget structure, which also holds a list of
         * all the child Components to that Widget.
         * If a Component has child Components, they are all recursively added to the list
         * of child Components, belonging to that Component.         * 
         */
        public void init() {
            widgets = ctx.widgets.getLoaded();
            for (Widget widget : widgets) {
                if (widget.isValid() && widget.getComponentCount() > 0) {
                    final AWidgetData awiddata = new AWidgetData(widget.getIndex());
                    widgetList.add(awiddata);
                    for (org.powerbot.script.wrappers.Component curComp : widget.getComponents()) {
                        final AComponentData curComponentData = new AComponentData(curComp);
                        awiddata.childComponents.add(curComponentData);
                        if (curComp.getChildrenCount() > 0) {
                            curComponentData.childComponents = new ArrayList<>();
                            recursiveAdd(curComponentData.childComponents, 1, curComp.getChildren());
                        }
                    }
                }
            }
        }

        /**
         * 
         * @param curCompDataList List of Components. New Components are added to that list.
         * @param depth Depth in the Component hierarchy, zero to n-1.
         * @param components Array of Components to be added to the list of Components.
         */
        final public void recursiveAdd(final ArrayList<AComponentData> curCompDataList, final int depth, final org.powerbot.script.wrappers.Component... components) {
            for (org.powerbot.script.wrappers.Component curComp : components) {
                final AComponentData newCompData = new AComponentData(curComp);
                newCompData.depth = depth;
                curCompDataList.add(newCompData);
                if (curComp.getChildrenCount() > 0) {
                    newCompData.childComponents = new ArrayList<>();
                    recursiveAdd(newCompData.childComponents, depth + 1, curComp.getChildren());
                }
            }
        }

        /**
         * 
         * @param s Search string.
         * @return List of SearchData.
         */
        final public ArrayList<SearchData> search(final String s) {
            final String lowerCased = s.toLowerCase();
            final ArrayList<SearchData> searchDataList = new ArrayList<>();
            for (AWidgetData widgetData : widgetList) {
                final int wIndex = widgetData.widgetIndex;
                for (AComponentData curCompData : widgetData.childComponents) {
                    final int cIndex = curCompData.comp.getIndex();
                    final org.powerbot.script.wrappers.Component curComp = curCompData.comp.getParent();
                    final int parentIndex = curComp == null ? -1 : curComp.getIndex();
                    if (curCompData.hasString(lowerCased)) {
                        searchDataList.add(new SearchData(wIndex, cIndex, parentIndex));
                    }
                    if (curCompData.comp.getChildrenCount() > 0) {
                        recursiveSearch(searchDataList, widgetData.widgetIndex, curCompData.childComponents, lowerCased);
                    }
                }
            }
            return searchDataList;
        }

        /**
         * 
         * @param searchDataList List of SearchData. Components that contains the search string,
         * are put in a new SearchData structure and added to the searchDataList.
         * @param wIndex Widget index number.
         * @param compDataList List of Components.
         * @param searchString String to search for.
         */
        final public void recursiveSearch(final ArrayList<SearchData> searchDataList, final int wIndex, final ArrayList<AComponentData> compDataList, final String searchString) {
            for (AComponentData curComp : compDataList) {
                final int cIndex = curComp.comp.getIndex();
                final org.powerbot.script.wrappers.Component parentComp = curComp.comp.getParent();
                final int parentIndex = parentComp == null ? -1 : parentComp.getIndex();
                if (curComp.hasString(searchString)) {
                    searchDataList.add(new SearchData(wIndex, cIndex, parentIndex));
                }
                if (curComp.comp.getChildrenCount() > 0) {
                    recursiveSearch(searchDataList, wIndex, curComp.childComponents, searchString);
                }
            }
        }
    }
}
