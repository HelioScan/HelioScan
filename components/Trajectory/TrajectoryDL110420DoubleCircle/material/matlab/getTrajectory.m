function [Tx,Ty] = getTrajectory(N,width)
% [Tx,Ty] = getTrajectory(N,width)
%
% Computes a double circle trajectory:
% uses six points to (approximately) set the two circles,
% and a seventh point to locate the crossing point of the two circles.
%
% INPUT:
% width   ... smaller values give a smoother change from the first to the 
%             second circle. Values between 5 and 10 should be ok.
% N       ... number of points used for defining the trajectory
%
% OUTPUT:
% [Tx,Ty] ... two vectors of length N with the points of the trajectory. 
%             The trajectory starts at the crossing point and rotates in
%             the positive direction. First it does the outer circle then
%             the inner circle.

tiffname = '2011_01_06__15_21_18h__channel00';
npoints = 7;

[stack,nfiles] = tiffread2(tiffname);
img = stack.data;

imagesc(img);
axis('equal')
set(gca, 'xlim', [0 size(img,1)], 'ylim', [0 size(img,2)]);

x = zeros(1,npoints);
y = x;

% set inner and outer points of double circle
i = 1;
disp('first branch: set first point')
[x(i),y(i)] = ginput(1);
line(x(i), y(i), 'linestyle', 'none', 'marker', 'o', 'markersize', 4, 'color', 'r')

i = 2;
disp('first branch: set second point')
[x(i),y(i)] = ginput(1);
line(x(i), y(i), 'linestyle', 'none', 'marker', 'o', 'markersize', 4, 'color', 'r')

i = 3;
disp('second branch: set first point')
[x(i),y(i)] = ginput(1);
line(x(i), y(i), 'linestyle', 'none', 'marker', 'o', 'markersize', 4, 'color', 'r')

i = 4;
disp('second branch: set second point')
[x(i),y(i)] = ginput(1);
line(x(i), y(i), 'linestyle', 'none', 'marker', 'o', 'markersize', 4, 'color', 'r')

i = 5;
disp('third branch: set first point')
[x(i),y(i)] = ginput(1);
line(x(i), y(i), 'linestyle', 'none', 'marker', 'o', 'markersize', 4, 'color', 'r')

i = 6;
disp('third branch: set second point')
[x(i),y(i)] = ginput(1);
line(x(i), y(i), 'linestyle', 'none', 'marker', 'o', 'markersize', 4, 'color', 'r')

%% determine centerline trajectory
xc1 = mean(x(1:2));
yc1 = mean(y(1:2));

xc2 = mean(x(3:4));
yc2 = mean(y(3:4));

xc3 = mean(x(5:6));
yc3 = mean(y(5:6));

ax = xc2-xc1;
ay = yc2-yc1;

bx = xc3-xc1;
by = yc3-yc1;

A = [ 1   0  -1   0;
      0   1   0  -1;
      ax  ay  0   0; 
      0   0  bx  by];
  
rhs = [bx/2 - ax/2;
       by/2 - ay/2;
       0;
       0];
    
p = A\rhs;
  
% centre of the trajectory
xc = xc1 + ax/2 + p(1);
yc = yc1 + ay/2 + p(2);

% radius of centerline trajectory
Rc = norm([xc1-xc;yc1-yc]);

% radial offset of double circle

deltaR = mean( [norm([xc1-x(1);yc1-y(1)]);
                norm([xc2-x(3);yc2-y(3)]);
                norm([xc3-x(5);yc3-y(5)])]);

          
%% compute centreline trajectory
t = linspace(0,2*pi,N+1);
t = t(1:end-1);

Tcx = xc + Rc*cos(t);
Tcy = yc + Rc*sin(t);

line(Tcx, Tcy, 'linestyle', '--', 'color', 'r')


%% indicate crossing point
i = 7;
disp('indicate crossing point')
[x(i),y(i)] = ginput(1);
line(x(i), y(i), 'linestyle', 'none', 'marker', 'o', 'markersize', 4, 'color', 'r')

% compute phase shift
deltat = atan((y(7)- yc)/(x(7) - xc));
if (x(7) < xc)
    deltat = deltat + pi;
end

%% compute final trajectory
R  = Rc + deltaR*(tanh(width*t) - tanh(width*(t-pi)) + tanh(width*(t-2*pi)));
Tx = xc + R.*cos(2*t+deltat);
Ty = yc + R.*sin(2*t+deltat);

line(Tx, Ty, 'linestyle', '-', 'color', 'r')
