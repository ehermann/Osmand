package net.osmand.plus.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.MapMarkersHelper.MapMarkersGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import net.osmand.plus.base.PointImageDrawable;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu.TrackChartPoints;
import net.osmand.plus.render.OsmandRenderer;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.plus.settings.backend.OsmandSettings.CommonPreference;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.ContextMenuLayer.IMoveObjectProvider;
import net.osmand.plus.views.MapTextLayer.MapTextProvider;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.dialogs.ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR;
import static net.osmand.plus.dialogs.ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR;

public class GPXLayer extends OsmandMapLayer implements IContextMenuProvider, IMoveObjectProvider, MapTextProvider<WptPt> {

	private static final double TOUCH_RADIUS_MULTIPLIER = 1.5;
	private static final int START_ZOOM = 7;

	private OsmandMapTileView view;

	private Paint paint;
	private Paint paint2;
	private boolean isPaint2;
	private Paint shadowPaint;
	private boolean isShadowPaint;
	private Paint paint_1;
	private boolean isPaint_1;
	private int cachedHash;
	private int cachedColor;
	private Paint paintIcon;
	private int currentTrackColor;

	private LayerDrawable selectedPoint;
	private TrackChartPoints trackChartPoints;

	private GpxSelectionHelper selectedGpxHelper;
	private MapMarkersHelper mapMarkersHelper;
	private Paint paintBmp;
	private List<WptPt> cache = new ArrayList<>();
	private Map<WptPt, SelectedGpxFile> pointFileMap = new HashMap<>();
	private MapTextLayer textLayer;

	private Paint paintOuterRect;
	private Paint paintInnerRect;

	private Paint paintGridOuterCircle;
	private Paint paintGridCircle;

	private Paint paintTextIcon;

	private OsmandRenderer osmandRenderer;

	private ContextMenuLayer contextMenuLayer;
	@ColorInt
	private int visitedColor;
	@ColorInt
	private int defPointColor;
	@ColorInt
	private int grayColor;

	private CommonPreference<String> currentTrackColorPref;
	private CommonPreference<String> currentTrackWidthPref;

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		selectedGpxHelper = view.getApplication().getSelectedGpxHelper();
		mapMarkersHelper = view.getApplication().getMapMarkersHelper();
		osmandRenderer = view.getApplication().getResourceManager().getRenderer().getRenderer();
		currentTrackColorPref = view.getSettings().getCustomRenderProperty(CURRENT_TRACK_COLOR_ATTR);
		currentTrackWidthPref = view.getSettings().getCustomRenderProperty(CURRENT_TRACK_WIDTH_ATTR);
		initUI();
	}

	public void setTrackChartPoints(TrackChartPoints trackChartPoints) {
		this.trackChartPoints = trackChartPoints;
	}

	private void initUI() {
		paint = new Paint();
		paint.setStyle(Style.STROKE);
		paint.setAntiAlias(true);
		paint2 = new Paint();
		paint2.setStyle(Style.STROKE);
		paint2.setAntiAlias(true);
		shadowPaint = new Paint();
		shadowPaint.setStyle(Style.STROKE);
		shadowPaint.setAntiAlias(true);
		paint_1 = new Paint();
		paint_1.setStyle(Style.STROKE);
		paint_1.setAntiAlias(true);

		paintBmp = new Paint();
		paintBmp.setAntiAlias(true);
		paintBmp.setFilterBitmap(true);
		paintBmp.setDither(true);

		paintTextIcon = new Paint();
		paintTextIcon.setTextSize(10 * view.getDensity());
		paintTextIcon.setTextAlign(Align.CENTER);
		paintTextIcon.setFakeBoldText(true);
//		paintTextIcon.setColor(Color.WHITE);
		paintTextIcon.setAntiAlias(true);

		textLayer = view.getLayerByClass(MapTextLayer.class);

		paintInnerRect = new Paint();
		paintInnerRect.setStyle(Style.FILL);
		paintInnerRect.setAntiAlias(true);
		paintOuterRect = new Paint();
		paintOuterRect.setStyle(Style.STROKE);
		paintOuterRect.setAntiAlias(true);
//		paintOuterRect.setColor(Color.WHITE);
		paintOuterRect.setStrokeWidth(3);
		paintOuterRect.setAlpha(255);
		paintGridCircle = new Paint();
		paintGridCircle.setStyle(Style.FILL_AND_STROKE);
		paintGridCircle.setAntiAlias(true);
        paintGridOuterCircle = new Paint();
        paintGridOuterCircle.setStyle(Style.FILL_AND_STROKE);
        paintGridOuterCircle.setAntiAlias(true);
        paintGridOuterCircle.setColor(Color.WHITE);
        paintGridOuterCircle.setAlpha(204);

		paintIcon = new Paint();
		selectedPoint = (LayerDrawable) AppCompatResources.getDrawable(view.getContext(), R.drawable.map_location_default);

		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);

		visitedColor = ContextCompat.getColor(view.getApplication(), R.color.color_ok);
		defPointColor = ContextCompat.getColor(view.getApplication(), R.color.gpx_color_point);
		grayColor = ContextCompat.getColor(view.getApplication(), R.color.color_favorite_gray);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (contextMenuLayer.getMoveableObject() instanceof WptPt) {
			WptPt objectInMotion = (WptPt) contextMenuLayer.getMoveableObject();
			SelectedGpxFile gpxFile = pointFileMap.get(objectInMotion);
			if (gpxFile != null) {
				PointF pf = contextMenuLayer.getMovableCenterPoint(tileBox);
				MapMarker mapMarker = mapMarkersHelper.getMapMarker(objectInMotion);
				float textScale = view.getSettings().TEXT_SCALE.get();
				drawBigPoint(canvas, objectInMotion, getFileColor(gpxFile), pf.x, pf.y, mapMarker, textScale);
			}
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		List<SelectedGpxFile> selectedGPXFiles = selectedGpxHelper.getSelectedGPXFiles();
		cache.clear();
		currentTrackColor = view.getSettings().CURRENT_TRACK_COLOR.get();
		if (!selectedGPXFiles.isEmpty()) {
			drawSelectedFilesSegments(canvas, tileBox, selectedGPXFiles, settings);
			canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
			if (trackChartPoints != null) {
				drawXAxisPoints(canvas, tileBox);
			}
			drawSelectedFilesSplits(canvas, tileBox, selectedGPXFiles, settings);
			drawSelectedFilesPoints(canvas, tileBox, selectedGPXFiles);
		}
		if (textLayer != null && isTextVisible()) {
			textLayer.putData(this, cache);
		}

	}

	private int updatePaints(int color, boolean routePoints, boolean currentTrack, DrawSettings nightMode, RotatedTileBox tileBox) {
		RenderingRulesStorage rrs = view.getApplication().getRendererRegistry().getCurrentSelectedRenderer();
		final boolean isNight = nightMode != null && nightMode.isNightMode();
		int hsh = calculateHash(rrs, routePoints, isNight, tileBox.getMapDensity(), tileBox.getZoom(),
			currentTrackColorPref.get(), currentTrackWidthPref.get());
		if (hsh != cachedHash) {
			cachedHash = hsh;
			cachedColor = ContextCompat.getColor(view.getApplication(), R.color.gpx_track);
			if (rrs != null) {
				RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
				req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, isNight);
				CommonPreference<String> p = view.getSettings().getCustomRenderProperty(CURRENT_TRACK_COLOR_ATTR);
				if (p != null && p.isSet()) {
					RenderingRuleProperty ctColor = rrs.PROPS.get(CURRENT_TRACK_COLOR_ATTR);
					if (ctColor != null) {
						req.setStringFilter(ctColor, p.get());
					}
				}
				CommonPreference<String> p2 = view.getSettings().getCustomRenderProperty(CURRENT_TRACK_WIDTH_ATTR);
				if (p2 != null && p2.isSet()) {
					RenderingRuleProperty ctWidth = rrs.PROPS.get(CURRENT_TRACK_WIDTH_ATTR);
					if (ctWidth != null) {
						req.setStringFilter(ctWidth, p2.get());
					}
				}
				String additional = "";
				if (routePoints) {
					additional = "routePoints=true";
				}
				if (currentTrack) {
					additional = (additional.length() == 0 ? "" : ";") + "currentTrack=true";
				}
				req.setIntFilter(rrs.PROPS.R_MINZOOM, tileBox.getZoom());
				req.setIntFilter(rrs.PROPS.R_MAXZOOM, tileBox.getZoom());
				if (additional.length() > 0) {
					req.setStringFilter(rrs.PROPS.R_ADDITIONAL, additional);
				}
				if (req.searchRenderingAttribute("gpx")) {
					RenderingContext rc = new OsmandRenderer.RenderingContext(view.getContext());
					rc.setDensityValue((float) tileBox.getMapDensity());
					cachedColor = req.getIntPropertyValue(rrs.PROPS.R_COLOR);
					osmandRenderer.updatePaint(req, paint, 0, false, rc);
					isPaint2 = osmandRenderer.updatePaint(req, paint2, 1, false, rc);
					isPaint_1 = osmandRenderer.updatePaint(req, paint_1, -1, false, rc);
					isShadowPaint = req.isSpecified(rrs.PROPS.R_SHADOW_RADIUS);
					if (isShadowPaint) {
						ColorFilter cf = new PorterDuffColorFilter(req.getIntPropertyValue(rrs.PROPS.R_SHADOW_COLOR),
								Mode.SRC_IN);
						shadowPaint.setColorFilter(cf);
						shadowPaint.setStrokeWidth(paint.getStrokeWidth() + 2
								* rc.getComplexValue(req, rrs.PROPS.R_SHADOW_RADIUS));
					}
				} else {
					System.err.println("Rendering attribute gpx is not found !");
					paint.setStrokeWidth(7 * view.getDensity());
				}
			}
		}
		paint.setColor(color == 0 ? cachedColor : color);
		return cachedColor;
	}

	private int calculateHash(Object... o) {
		return Arrays.hashCode(o);
	}

	private void drawSelectedFilesSplits(Canvas canvas, RotatedTileBox tileBox, List<SelectedGpxFile> selectedGPXFiles,
										 DrawSettings settings) {
		if (tileBox.getZoom() >= START_ZOOM) {
			// request to load
			for (SelectedGpxFile g : selectedGPXFiles) {
				List<GpxDisplayGroup> groups = g.getDisplayGroups(view.getApplication());
				if (groups != null && !groups.isEmpty()) {
					int color = g.getGpxFile().getColor(0);
					if (color == 0) {
						color = g.getModifiableGpxFile().getColor(0);
					}
					if (color == 0) {
						color = cachedColor;
					}

					paintInnerRect.setColor(color);
					paintInnerRect.setAlpha(179);

					paintTextIcon.setColor(txtlabelColor(color));
					paintOuterRect.setColor(txtlabelColor(color));

					List<GpxDisplayItem> items = groups.get(0).getModifiableList();

					drawSplitItems(canvas, tileBox, items, settings);
				}
			}
		}
	}

	private int txtlabelColor(int color) {
		//Hardy, 2020-03-16: Contrast logic for text labels on tracks
		if (((int) Color.red(color) * .299 + Color.green(color) * .587 + Color.blue(color) * .114) > 149) {
			return Color.BLACK;
		}
		return Color.WHITE;
	}

	private void drawSplitItems(Canvas canvas, RotatedTileBox tileBox, List<GpxDisplayItem> items, DrawSettings settings) {
		final QuadRect latLonBounds = tileBox.getLatLonBounds();
		int r = (int) (12 * tileBox.getDensity());
		paintTextIcon.setTextSize(r);
		int dr = r * 3 / 2;
		int px = -1;
		int py = -1;
		for (int k = 0; k < items.size(); k++) {
			GpxDisplayItem i = items.get(k);
			WptPt o = i.locationEnd;
			if (o != null && o.lat >= latLonBounds.bottom && o.lat <= latLonBounds.top && o.lon >= latLonBounds.left
					&& o.lon <= latLonBounds.right) {
				int x = (int) tileBox.getPixXFromLatLon(o.lat, o.lon);
				int y = (int) tileBox.getPixYFromLatLon(o.lat, o.lon);
				if (px != -1 || py != -1) {
					if (Math.abs(x - px) <= dr && Math.abs(y - py) <= dr) {
						continue;
					}
				}
				px = x;
				py = y;
				String nm = i.splitName;
				if (nm != null) {
					int ind = nm.indexOf(' ');
					if (ind > 0) {
						nm = nm.substring(0, ind);
					}
					Rect bounds = new Rect();
					paintTextIcon.getTextBounds(nm, 0, nm.length(), bounds);
					int nmWidth = bounds.width();
					int nmHeight = bounds.height();
                    RectF rect = new RectF(x - nmWidth / 2 - 2 * (float) Math.ceil(tileBox.getDensity()),
                            y + nmHeight / 2 + 3 * (float) Math.ceil(tileBox.getDensity()),
                            x + nmWidth / 2 + 3 * (float) Math.ceil(tileBox.getDensity()),
                            y - nmHeight / 2 - 2 * (float) Math.ceil(tileBox.getDensity()));
                    canvas.drawRoundRect(rect, 0, 0, paintInnerRect);
					canvas.drawRoundRect(rect, 0, 0, paintOuterRect);
//					canvas.drawRect(rect, paintInnerRect);
//					canvas.drawRect(rect, paintOuterRect);
					canvas.drawText(nm, x, y + nmHeight / 2, paintTextIcon);
				}
			}
		}
	}

	private void drawSelectedFilesPoints(Canvas canvas, RotatedTileBox tileBox, List<SelectedGpxFile> selectedGPXFiles) {
		if (tileBox.getZoom() >= START_ZOOM) {
			float textScale = view.getSettings().TEXT_SCALE.get();
			float iconSize = getIconSize(view.getContext()) * 3 / 2.5f * textScale;
			QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);

			List<LatLon> fullObjectsLatLon = new ArrayList<>();
			List<LatLon> smallObjectsLatLon = new ArrayList<>();
			Map<WptPt, SelectedGpxFile> pointFileMap = new HashMap<>();
			// request to load
			final QuadRect latLonBounds = tileBox.getLatLonBounds();
			for (SelectedGpxFile g : selectedGPXFiles) {
				List<Pair<WptPt, MapMarker>> fullObjects = new ArrayList<>();
				int fileColor = getFileColor(g);
				boolean synced = mapMarkersHelper.getMarkersGroup(g.getGpxFile()) != null;
				for (WptPt wpt : getListStarPoints(g)) {
					if (wpt.lat >= latLonBounds.bottom && wpt.lat <= latLonBounds.top
							&& wpt.lon >= latLonBounds.left && wpt.lon <= latLonBounds.right
							&& wpt != contextMenuLayer.getMoveableObject()) {
						pointFileMap.put(wpt, g);
						MapMarker marker = null;
						if (synced) {
							if ((marker = mapMarkersHelper.getMapMarker(wpt)) == null) {
								continue;
							}
						}
						cache.add(wpt);
						float x = tileBox.getPixXFromLatLon(wpt.lat, wpt.lon);
						float y = tileBox.getPixYFromLatLon(wpt.lat, wpt.lon);

						if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
							@ColorInt
							int color;
							if (marker != null && marker.history) {
								color = grayColor;
							} else {
								color = getPointColor(wpt, fileColor);
							}
							PointImageDrawable pointImageDrawable = PointImageDrawable.getFromWpt(view.getContext(), color,
									true, wpt);
							pointImageDrawable.drawSmallPoint(canvas, x, y, textScale);
							smallObjectsLatLon.add(new LatLon(wpt.lat, wpt.lon));
						} else {
							fullObjects.add(new Pair<>(wpt, marker));
							fullObjectsLatLon.add(new LatLon(wpt.lat, wpt.lon));
						}
					}
					if (wpt == contextMenuLayer.getMoveableObject()) {
						pointFileMap.put(wpt, g);
					}
				}
				for (Pair<WptPt, MapMarker> pair : fullObjects) {
					WptPt wpt = pair.first;
					float x = tileBox.getPixXFromLatLon(wpt.lat, wpt.lon);
					float y = tileBox.getPixYFromLatLon(wpt.lat, wpt.lon);
					drawBigPoint(canvas, wpt, fileColor, x, y, pair.second, textScale);
				}
			}
			if (trackChartPoints != null) {
				LatLon highlightedPoint = trackChartPoints.getHighlightedPoint();
				if (highlightedPoint != null) {
					if (highlightedPoint.getLatitude() >= latLonBounds.bottom
							&& highlightedPoint.getLatitude() <= latLonBounds.top
							&& highlightedPoint.getLongitude() >= latLonBounds.left
							&& highlightedPoint.getLongitude() <= latLonBounds.right) {
						float x = tileBox.getPixXFromLatLon(highlightedPoint.getLatitude(), highlightedPoint.getLongitude());
						float y = tileBox.getPixYFromLatLon(highlightedPoint.getLatitude(), highlightedPoint.getLongitude());
						paintIcon.setColorFilter(null);
						selectedPoint.setBounds((int) x - selectedPoint.getIntrinsicWidth() / 2,
								(int) y - selectedPoint.getIntrinsicHeight() / 2,
								(int) x + selectedPoint.getIntrinsicWidth() / 2,
								(int) y + selectedPoint.getIntrinsicHeight() / 2);
						selectedPoint.draw(canvas);
					}
				}
			}
			this.fullObjectsLatLon = fullObjectsLatLon;
			this.smallObjectsLatLon = smallObjectsLatLon;
			this.pointFileMap = pointFileMap;
		}
	}

	private void drawXAxisPoints(Canvas canvas, RotatedTileBox tileBox) {
		int color = trackChartPoints.getSegmentColor();
		if (color == 0) {
			color = trackChartPoints.getGpx().getColor(0);
			if (trackChartPoints.getGpx().showCurrentTrack) {
				color = currentTrackColor;
			}
			if (color == 0) {
				color = cachedColor;
			}
			trackChartPoints.setSegmentColor(color);
		}
		paintGridCircle.setColor(color);
        paintGridCircle.setAlpha(255);
		QuadRect latLonBounds = tileBox.getLatLonBounds();
		float r = 3 * tileBox.getDensity();
		List<LatLon> xAxisPoints = trackChartPoints.getXAxisPoints();
		if (xAxisPoints != null) {
			float density = (float) Math.ceil(tileBox.getDensity());
			float outerRadius = r + 2 * density;
			float innerRadius = r + density;
			QuadRect prevPointRect = null;
			for (int i = 0; i < xAxisPoints.size(); i++) {
				LatLon axisPoint = xAxisPoints.get(i);
				if (axisPoint != null) {
					if (axisPoint.getLatitude() >= latLonBounds.bottom
							&& axisPoint.getLatitude() <= latLonBounds.top
							&& axisPoint.getLongitude() >= latLonBounds.left
							&& axisPoint.getLongitude() <= latLonBounds.right) {
						float x = tileBox.getPixXFromLatLon(axisPoint.getLatitude(), axisPoint.getLongitude());
						float y = tileBox.getPixYFromLatLon(axisPoint.getLatitude(), axisPoint.getLongitude());
						QuadRect pointRect = new QuadRect(x - outerRadius, y - outerRadius, x + outerRadius, y + outerRadius);
						if (prevPointRect == null || !QuadRect.intersects(prevPointRect, pointRect)) {
							canvas.drawCircle(x, y, outerRadius, paintGridOuterCircle);
							canvas.drawCircle(x, y, innerRadius, paintGridCircle);
							prevPointRect = pointRect;
						}
					}
				}
			}
		}
	}

	private int getFileColor(@NonNull SelectedGpxFile g) {
		return g.getColor() == 0 ? defPointColor : g.getColor();
	}

	private void drawBigPoint(Canvas canvas, WptPt wpt, int fileColor, float x, float y, @Nullable MapMarker marker, float textScale) {
		int pointColor = getPointColor(wpt, fileColor);
		PointImageDrawable pointImageDrawable;
		boolean history = false;
		if (marker != null) {
			pointImageDrawable = PointImageDrawable.getOrCreateSyncedIcon(view.getContext(), pointColor, wpt);
			history = marker.history;
		} else {
			pointImageDrawable = PointImageDrawable.getFromWpt(view.getContext(), pointColor, true, wpt);
		}
		pointImageDrawable.drawPoint(canvas, x, y, textScale, history);
	}

	@ColorInt
	private int getPointColor(WptPt o, @ColorInt int fileColor) {
		boolean visit = isPointVisited(o);
		return visit ? visitedColor : o.getColor(fileColor);
	}

	private void drawSelectedFilesSegments(Canvas canvas, RotatedTileBox tileBox,
										   List<SelectedGpxFile> selectedGPXFiles, DrawSettings settings) {

		SelectedGpxFile currentTrack = null;
		for (SelectedGpxFile g : selectedGPXFiles) {
			if (g.isShowCurrentTrack()) {
				currentTrack = g;
			} else {
				drawSelectedFileSegments(g, false, canvas, tileBox, settings);
			}
		}
		if (currentTrack != null) {
			drawSelectedFileSegments(currentTrack, true, canvas, tileBox, settings);
		}
	}

	private void drawSelectedFileSegments(SelectedGpxFile selectedGpxFile, boolean currentTrack, Canvas canvas,
										  RotatedTileBox tileBox, DrawSettings settings) {
		List<TrkSegment> segments = selectedGpxFile.getPointsToDisplay();
		for (TrkSegment ts : segments) {
			int color = selectedGpxFile.getGpxFile().getColor(0);
			if (currentTrack) {
				color = currentTrackColor;
			}
			if (color == 0) {
				color = ts.getColor(cachedColor);
			}
			if (ts.renderer == null && !ts.points.isEmpty()) {
				if (currentTrack) {
					ts.renderer = new Renderable.CurrentTrack(ts.points);
				} else {
					ts.renderer = new Renderable.StandardTrack(ts.points, 17.2);
				}
			}
			updatePaints(color, selectedGpxFile.isRoutePoints(), currentTrack, settings, tileBox);
			if(ts.renderer instanceof Renderable.RenderableSegment) {
				((Renderable.RenderableSegment) ts.renderer).drawSegment(view.getZoom(), paint, canvas, tileBox);
			}
		}
	}

	private boolean isPointVisited(WptPt o) {
		boolean visit = false;
		String visited = o.getExtensionsToRead().get("VISITED_KEY");
		if (visited != null && !visited.equals("0")) {
			visit = true;
		}
		return visit;
	}

	private List<WptPt> getListStarPoints(SelectedGpxFile g) {
		return g.getGpxFile().getPoints();
	}

	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return (Math.abs(objx - ex) <= radius && Math.abs(objy - ey) <= radius);
	}

	public void getWptFromPoint(RotatedTileBox tb, PointF point, List<? super WptPt> res) {
		int r = (int) (getScaledTouchRadius(view.getApplication(), getDefaultRadiusPoi(tb)) * TOUCH_RADIUS_MULTIPLIER);
		int ex = (int) point.x;
		int ey = (int) point.y;
		List<SelectedGpxFile> selectedGpxFiles = new ArrayList<>(selectedGpxHelper.getSelectedGPXFiles());
		for (SelectedGpxFile g : selectedGpxFiles) {
			List<WptPt> pts = getListStarPoints(g);
			// int fcolor = g.getColor() == 0 ? clr : g.getColor();
			for (WptPt n : pts) {
				int x = (int) tb.getPixXFromLatLon(n.lat, n.lon);
				int y = (int) tb.getPixYFromLatLon(n.lat, n.lon);
				if (calculateBelongs(ex, ey, x, y, r)) {
					res.add(n);
				}
			}
		}
	}

	public void getTracksFromPoint(RotatedTileBox tb, PointF point, List<Object> res) {
		int r = getScaledTouchRadius(view.getApplication(), getDefaultRadiusPoi(tb));
		int mx = (int) point.x;
		int my = (int) point.y;
		List<SelectedGpxFile> selectedGpxFiles = new ArrayList<>(selectedGpxHelper.getSelectedGPXFiles());
		for (SelectedGpxFile selectedGpxFile : selectedGpxFiles) {
			List<TrkSegment> segments = selectedGpxFile.getPointsToDisplay();
			for (TrkSegment segment : segments) {
				QuadRect trackBounds = GPXUtilities.calculateBounds(segment.points);
				if (QuadRect.trivialOverlap(tb.getLatLonBounds(), trackBounds)) {
					SelectedGpxPoint selectedGpxPoint = findPointNearSegment(tb, selectedGpxFile, segment.points, r, mx, my);
					if (selectedGpxPoint != null) {
						res.add(selectedGpxPoint);
						break;
					}
				}
			}
		}
	}

	private SelectedGpxPoint findPointNearSegment(RotatedTileBox tb, SelectedGpxFile selectedGpxFile, List<WptPt> points, int r, int mx, int my) {
		WptPt firstPoint = points.get(0);
		int ppx = (int) tb.getPixXFromLatLon(firstPoint.lat, firstPoint.lon);
		int ppy = (int) tb.getPixYFromLatLon(firstPoint.lat, firstPoint.lon);
		int pcross = placeInBbox(ppx, ppy, mx, my, r, r);

		for (int i = 1; i < points.size(); i++) {
			WptPt point = points.get(i);
			int px = (int) tb.getPixXFromLatLon(point.lat, point.lon);
			int py = (int) tb.getPixYFromLatLon(point.lat, point.lon);
			int cross = placeInBbox(px, py, mx, my, r, r);
			if (cross == 0) {
				return createProjectionPoint(selectedGpxFile, points.get(i - 1), point, tb.getLatLonFromPixel(mx, my));
			}
			if ((pcross & cross) == 0) {
				int mpx = px;
				int mpy = py;
				int mcross = cross;
				while (Math.abs(mpx - ppx) > r || Math.abs(mpy - ppy) > r) {
					int mpxnew = mpx / 2 + ppx / 2;
					int mpynew = mpy / 2 + ppy / 2;
					int mcrossnew = placeInBbox(mpxnew, mpynew, mx, my, r, r);
					if (mcrossnew == 0) {
						return createProjectionPoint(selectedGpxFile, points.get(i - 1), point, tb.getLatLonFromPixel(mx, my));
					}
					if ((mcrossnew & mcross) != 0) {
						mpx = mpxnew;
						mpy = mpynew;
						mcross = mcrossnew;
					} else if ((mcrossnew & pcross) != 0) {
						ppx = mpxnew;
						ppy = mpynew;
						pcross = mcrossnew;
					} else {
						// this should never happen theoretically
						break;
					}
				}
			}
			pcross = cross;
			ppx = px;
			ppy = py;
		}
		return null;
	}

	private SelectedGpxPoint createProjectionPoint(SelectedGpxFile selectedGpxFile, WptPt prevPoint, WptPt nextPoint, LatLon latLon) {
		LatLon projection = MapUtils.getProjection(latLon.getLatitude(), latLon.getLongitude(), prevPoint.lat, prevPoint.lon, nextPoint.lat, nextPoint.lon);

		WptPt projectionPoint = new WptPt();
		projectionPoint.lat = projection.getLatitude();
		projectionPoint.lon = projection.getLongitude();
		projectionPoint.heading = prevPoint.heading;
		projectionPoint.distance = prevPoint.distance + MapUtils.getDistance(projection, prevPoint.lat, prevPoint.lon);
		projectionPoint.ele = getValueByDistInterpolation(projectionPoint.distance, prevPoint.distance, prevPoint.ele, nextPoint.distance, nextPoint.ele);
		projectionPoint.speed = getValueByDistInterpolation(projectionPoint.distance, prevPoint.distance, prevPoint.speed, nextPoint.distance, nextPoint.speed);
		if (prevPoint.time != 0 && nextPoint.time != 0) {
			projectionPoint.time = (long) getValueByDistInterpolation(projectionPoint.distance, prevPoint.distance, prevPoint.time, nextPoint.distance, nextPoint.time);
		}

		Location prevPointLocation = new Location("");
		prevPointLocation.setLatitude(prevPoint.lat);
		prevPointLocation.setLongitude(prevPoint.lon);

		Location nextPointLocation = new Location("");
		nextPointLocation.setLatitude(nextPoint.lat);
		nextPointLocation.setLongitude(nextPoint.lon);

		prevPointLocation.setBearing(prevPointLocation.bearingTo(nextPointLocation));

		return new SelectedGpxPoint(selectedGpxFile, projectionPoint, prevPointLocation);
	}

	private double getValueByDistInterpolation(double projectionDist, double prevDist, double prevVal, double nextDist, double nextVal) {
		return prevVal + (projectionDist - prevDist) * ((nextVal - prevVal) / (nextDist - prevDist));
	}

	int placeInBbox(int x, int y, int mx, int my, int halfw, int halfh) {
		int cross = 0;
		cross |= (x < mx - halfw ? 1 : 0);
		cross |= (x > mx + halfw ? 2 : 0);
		cross |= (y < my - halfh ? 4 : 0);
		cross |= (y > my + halfh ? 8 : 0);
		return cross;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof WptPt) {
			return new PointDescription(PointDescription.POINT_TYPE_WPT, ((WptPt) o).name);
		} else if (o instanceof SelectedGpxPoint) {
			SelectedGpxFile selectedGpxFile = ((SelectedGpxPoint) o).getSelectedGpxFile();
			String name;
			if (selectedGpxFile.isShowCurrentTrack()) {
				name = view.getContext().getString(R.string.shared_string_currently_recording_track);
			} else {
				name = formatName(Algorithms.getFileWithoutDirs(selectedGpxFile.getGpxFile().path));
			}
			return new PointDescription(PointDescription.POINT_TYPE_GPX, name);
		}
		return null;
	}

	private String formatName(String name) {
		int ext = name.lastIndexOf('.');
		if (ext != -1) {
			name = name.substring(0, ext);
		}
		return name.replace('_', ' ');
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap() {
		return false;
	}

	@Override
	public boolean isObjectClickable(Object o) {
		return o instanceof WptPt || o instanceof SelectedGpxFile;
	}

	@Override
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res, boolean unknownLocation) {
		if (tileBox.getZoom() >= START_ZOOM) {
			getWptFromPoint(tileBox, point, res);
			getTracksFromPoint(tileBox, point, res);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof WptPt) {
			return new LatLon(((WptPt) o).lat, ((WptPt) o).lon);
		} else if (o instanceof SelectedGpxPoint) {
			WptPt point = ((SelectedGpxPoint) o).getSelectedPoint();
			return new LatLon(point.lat, point.lon);
		}
		return null;
	}


	@Override
	public void destroyLayer() {

	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public LatLon getTextLocation(WptPt o) {
		return new LatLon(o.lat, o.lon);
	}

	@Override
	public int getTextShift(WptPt o, RotatedTileBox rb) {
		return (int) (16 * rb.getDensity());
	}

	@Override
	public String getText(WptPt o) {
		return o.name;
	}

	@Override
	public boolean isTextVisible() {
		return view.getSettings().SHOW_POI_LABEL.get();
	}

	@Override
	public boolean isFakeBoldText() {
		return false;
	}


	@Override
	public boolean isObjectMovable(Object o) {
		return o instanceof WptPt;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o,
									   @NonNull LatLon position,
									   @Nullable ContextMenuLayer.ApplyMovedObjectCallback callback) {

		if (o instanceof WptPt) {
			WptPt objectInMotion = (WptPt) o;
			SelectedGpxFile selectedGpxFile = pointFileMap.get(objectInMotion);
			if (selectedGpxFile != null) {
				GPXFile gpxFile = selectedGpxFile.getGpxFile();
				gpxFile.updateWptPt(objectInMotion, position.getLatitude(),
						position.getLongitude(), System.currentTimeMillis(), objectInMotion.desc,
						objectInMotion.name, objectInMotion.category, objectInMotion.getColor(),
						objectInMotion.getIconName(), objectInMotion.getBackgroundType());
				syncGpx(gpxFile);
				if (gpxFile.showCurrentTrack) {
					if (callback != null) {
						callback.onApplyMovedObject(true, objectInMotion);
					}
				} else {
					new SaveGpxFileAsyncTask(view.getApplication(), callback, objectInMotion).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, gpxFile);
				}
			}
		} else if (callback != null) {
			callback.onApplyMovedObject(false, o);
		}
	}

	private void syncGpx(GPXFile gpxFile) {
		MapMarkersGroup group = view.getApplication().getMapMarkersHelper().getMarkersGroup(gpxFile);
		if (group != null) {
			mapMarkersHelper.runSynchronization(group);	
		}
	}

	static class SaveGpxFileAsyncTask extends AsyncTask<GPXFile, Void, Exception> {
		private final OsmandApplication app;
		@Nullable
		private final ContextMenuLayer.ApplyMovedObjectCallback callback;
		@Nullable
		private final WptPt point;

		SaveGpxFileAsyncTask(OsmandApplication app,
							 @Nullable ContextMenuLayer.ApplyMovedObjectCallback callback,
							 @Nullable WptPt point) {
			this.app = app;
			this.callback = callback;
			this.point = point;
		}

		@Override
		protected Exception doInBackground(GPXFile... params) {
			GPXFile gpxFile = params[0];
			return GPXUtilities.writeGpxFile(new File(gpxFile.path), gpxFile);
		}

		@Override
		protected void onPostExecute(Exception errorMessage) {
			if (callback != null) {
				callback.onApplyMovedObject(errorMessage == null, point);
			}
		}
	}
}
