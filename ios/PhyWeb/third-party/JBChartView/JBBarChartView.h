//
//  JBBarChartView.h
//  JBChartView
//
//  Created by Terry Worona on 9/3/13.
//  Copyright (c) 2013 Jawbone. All rights reserved.
//

// Views
#import "JBChartView.h"

@class JBBarChartView;

@protocol JBBarChartViewDataSource <JBChartViewDataSource>

@required

/**
 *  The number of bars in a given bar chart is the number of vertical views shown along the x-axis.
 *
 *  @param barChartView     The bar chart object requesting this information.
 *
 *  @return Number of bars in the given chart, displayed horizontally along the chart's x-axis.
 */
- (NSUInteger)numberOfBarsInBarChartView:(JBBarChartView *)barChartView;

@optional

/**
 *  A UIView subclass representing the bar at a particular index.
 *
 *  Default: solid black UIView.
 *
 *  @param barChartView     The bar chart object requesting this information.
 *  @param index            The 0-based index of a given bar (left to right, x-axis).
 *
 *  @return A UIView subclass. The view will automatically be resized by the chart during creation (ie. no need to set the frame).
 */
- (UIView *)barChartView:(JBBarChartView *)barChartView barViewAtIndex:(NSUInteger)index;

@end

@protocol JBBarChartViewDelegate <JBChartViewDelegate>

@required

/**
 *  Height for a bar at a given index (left to right). There is no ceiling on the the height;
 *  the chart will automatically normalize all values between the overal min and max heights.
 *
 *  @param barChartView     The bar chart object requesting this information.
 *  @param index            The 0-based index of a given bar (left to right, x-axis).
 *
 *  @return The y-axis height of the supplied bar index (x-axis)
 */
- (CGFloat)barChartView:(JBBarChartView *)barChartView heightForBarViewAtIndex:(NSUInteger)index;

@optional

/**
 *  Occurs when a touch gesture event occurs on a given bar (chart must be expanded).
 *  and the selection must occur within the bounds of the chart.
 *
 *  @param barChartView     A bar chart object informing the delegate about the new selection.
 *  @param index            The 0-based index of a given bar (left to right, x-axis).
 *  @param touchPoint       The touch point in relation to the chart's bounds (excludes footer and header).
 */
- (void)barChartView:(JBBarChartView *)barChartView didSelectBarAtIndex:(NSUInteger)index touchPoint:(CGPoint)touchPoint;
- (void)barChartView:(JBBarChartView *)barChartView didSelectBarAtIndex:(NSUInteger)index;

/**
 *  Occurs when selection ends by either ending a touch event or selecting an area that is outside the view's bounds.
 *  For selection start events, see: didSelectBarAtIndex...
 *
 *  @param barChartView     A bar chart object informing the delegate about the deselection.
 */
- (void)didDeselectBarChartView:(JBBarChartView *)barChartView;

/**
 *  If you already implement barChartView:barViewAtIndex: delegate - this method has no effect.
 *  If a custom UIView isn't supplied, a flat bar will be made automatically (default color black).
 *
 *  Default: if none specified - calls barChartView:barViewAtIndex:.
 *
 *  @param barChartView     The bar chart object requesting this information.
 *  @param index            The 0-based index of a given bar (left to right, x-axis).
 *
 *  @return The color to be used to color a bar in the chart.
 */
- (UIColor *)barChartView:(JBBarChartView *)barChartView colorForBarViewAtIndex:(NSUInteger)index;

/**
 *  The selection color to be overlayed on a bar during touch events.
 *  The color is automatically faded to transparent (vertically). The property showsVerticalSelection
 *  must be YES for the color to apply.
 *
 *  Default: white color (faded to transparent).
 *
 *  @param barChartView     The bar chart object requesting this information.
 *
 *  @return The color to be used on each bar selection.
 */
- (UIColor *)barSelectionColorForBarChartView:(JBBarChartView *)barChartView;

/**
 *  Horizontal padding between bars.
 *
 *  Default: 'best-guess' algorithm based on the the total number of bars and width of the chart.
 *
 *  @param barChartView     The bar chart object requesting this information.
 *
 *  @return Horizontal width (in pixels) between each bar.
 */
- (CGFloat)barPaddingForBarChartView:(JBBarChartView *)barChartView;

@end

@interface JBBarChartView : JBChartView

@property (nonatomic, weak) id<JBBarChartViewDataSource> dataSource;
@property (nonatomic, weak) id<JBBarChartViewDelegate> delegate;

/**
 *  Vertical highlight overlayed on bar during touch events.
 *
 *  Default: YES.
 */
@property (nonatomic, assign) BOOL showsVerticalSelection;

/*
 *  Bars can be (vertically) positoned top to bottom instead of bottom up.
 *  If this property is set to YES, both the bar and the selection view will be inverted.
 *  For the inverted orientation to take effect, reloadData must be called.
 *
 *  Default: NO.
 */
@property (nonatomic, assign, getter=isInverted) BOOL inverted;

/**
 *  The bar view at a particular index.
 *
 *  Default: nil.
 *
 *  @param index            The 0-based index of a given bar (left to right, x-axis).
 *
 *  @return The UIView representing the bar view at a given index or nil if the index is out of range.
 */
- (UIView *)barViewAtIndex:(NSUInteger)index;

@end
