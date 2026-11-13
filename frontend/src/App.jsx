import { useState, useEffect, useRef } from 'react';
import './App.css';
import PrcConsole from './prc/PrcConsole';
import DeliveryConsole from './dlv/DeliveryConsole';
import LoginView from './dash/components/LoginView';
import MenuView from './dash/components/MenuView';
import RestaurantOrderView from './dash/components/RestaurantOrderView';
import {
  getCsrfToken,
  login,
  getCurrentUser,
  logout,
  register
} from './dash/services/authService';
import {
  getRestaurants,
  getMenu,
  placeOrder,
  getRestaurantOrders,
  acceptOrder,
  rejectOrder,
  startPreparation,
  markOrderReady,
  getCustomerOrders,
  cancelOrder,
  markItemSoldOut,
  markItemAvailable,
  pauseRestaurant,
  reopenRestaurant,
  getQueueSummary,
  resetSystem
} from './dash/services/orderService';
import CustomerOrderView from './dash/components/CustomerOrderView';
import QueueBand from './dash/components/QueueBand';
import InfoArea from './dash/components/InfoArea';

// First tab each role sees after login (R-DASH-01: only what the role may do)
function defaultTabFor(role) {
  if (role === 'RESTAURANT') return 'orders';
  if (role === 'COURIER' || role === 'OPERATOR') return 'delivery';
  return 'restaurants';
}

function App() {
  const restaurantCategories = {
  1: 'Other',
  2: 'Gluten-Free',
  3: 'Gluten-Free',
  4: 'Gluten-Free',
  5: 'Halal',
  6: 'Halal',
  7: 'Halal',
  8: 'Vegan',
  9: 'Vegan',
  10: 'Dairy-Free',
  11: 'Dairy-Free',
  12: 'Dairy-Free',
  13: 'Nut-Free',
  14: 'Nut-Free'
};

const categories = [
  'All',
  'Gluten-Free',
  'Halal',
  'Vegan',
  'Dairy-Free',
  'Nut-Free'
];
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [signingUp, setSigningUp] = useState(false);
  const [name, setName] = useState('');
  const [registrationPending, setRegistrationPending] = useState(false);
  const [message, setMessage] = useState('');
  const [loggedIn, setLoggedIn] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const [logs, setLogs] = useState([]);
  const [restaurants, setRestaurants] = useState([]);
  const [menu, setMenu] = useState([]);
  const [selectedRestaurantId, setSelectedRestaurantId] = useState(null);
  const [quantities, setQuantities] = useState({});
  const [orders, setOrders] = useState([]);
  const [activeTab, setActiveTab] = useState('restaurants');
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [pendingAction, setPendingAction] = useState(null);
  const [ordersLoading, setOrdersLoading] = useState(false);
  // In-page pop-up (browser pop-ups like window.alert can be blocked):
  // { title, message, returnTab } — a new one replaces the previous one, so pop-ups never stack
  const [notice, setNotice] = useState(null);
  // Customer's own orders (My Orders tab)
  const [customerOrders, setCustomerOrders] = useState([]);
  const [customerOrdersLoading, setCustomerOrdersLoading] = useState(false);
  // R-DASH-03: queued / preparing / ready counts per restaurant id
  const [queueSummary, setQueueSummary] = useState({});
  // Order currently being rejected, and the reason typed for it
  const [rejectingOrderId, setRejectingOrderId] = useState(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const actionInFlight = useRef(false);
  const orderLoadInFlight = useRef(null);

    useEffect(() => {
    if (loggedIn) {
      loadRestaurants();
      loadQueueSummary();
    }
  }, [loggedIn]);

  useEffect(() => {
    if (loggedIn && activeTab === 'orders') {
      loadRestaurantOrders();
    }
    if (loggedIn && activeTab === 'myorders') {
      loadCustomerOrders();
    }
    // Fresh queue colours every time the restaurant list is shown (R-DASH-03)
    if (loggedIn && activeTab === 'restaurants') {
      loadQueueSummary();
    }
  }, [loggedIn, activeTab]);

  function addLog(message) {
    setLogs(previous => [...previous, message]);
  }

  // Course feedback + UC-ORD-09: errors pop up in the user's face, and are still logged (R-DASH-06)
  function showError(title, errorMessage) {
    addLog(title + ': ' + errorMessage);
    setMessage(title + ': ' + errorMessage);
    setNotice({ title, message: errorMessage });
  }

  async function loadRestaurants() {
    try {
      const data = await getRestaurants();
      setRestaurants(data);
      addLog('Restaurants loaded');
    } catch (error) {
      showError('Error loading restaurants', error.message);
    }
  }

  // R-DASH-03 read model: counts per restaurant, keyed by restaurant id
  async function loadQueueSummary() {
    try {
      const data = await getQueueSummary();
      setQueueSummary(Object.fromEntries(data.map(entry => [entry.restaurantId, entry])));
    } catch (error) {
      showError('Error loading preparation queues', error.message);
    }
  }

  async function loadMenu(restaurantId) {
    try {
      const data = await getMenu(restaurantId);

      setMenu(data);
      setSelectedRestaurantId(restaurantId);
      setActiveTab('menu');

      addLog('Menu loaded for restaurant ' + restaurantId);
    } catch (error) {
      showError('Error loading menu', error.message);
    }
  }

  async function handlePlaceOrder(menuItemId) {
    try {
      const csrfToken = await getCsrfToken();

      const order = await placeOrder({
        restaurantId: selectedRestaurantId,
        orderLines: [
          {
            menuItemId: menuItemId,
            quantity: Number(quantities[menuItemId] ?? 1)
          }
        ]
      }, csrfToken);

      addLog('Order placed successfully: #' + order.orderId);
      setMessage('Order placed successfully: #' + order.orderId);
    } catch (error) {
      showError('Order placement failed', error.message);
    }
  }

  async function loadRestaurantOrders() {
    if (orderLoadInFlight.current) return orderLoadInFlight.current;
    const restaurantId = currentUser?.restaurantId;
    if (restaurantId == null) {
      setOrders([]);
      showError('Error loading orders', 'No restaurant is assigned to this account.');
      return;
    }
    setOrdersLoading(true);
    const request = (async () => {
      try {
        const data = await getRestaurantOrders(restaurantId);
        setOrders(data);
        addLog('Restaurant orders loaded for restaurant ' + restaurantId + ' (' + data.length + ' orders)');
      } catch (error) {
        showError('Error loading orders', error.message);
      } finally {
        setOrdersLoading(false);
      }
    })();
    orderLoadInFlight.current = request;
    try {
      await request;
    } finally {
      if (orderLoadInFlight.current === request) orderLoadInFlight.current = null;
    }
  }


// Runs one restaurant-order command; `command` is an orderService.js call that receives the CSRF token
async function runOrderAction(action, command) {
  // Prevent duplicate actions while another request is running
  if (actionInFlight.current) return;

  actionInFlight.current = true;
  setPendingAction(action);
  setMessage(action + ' in progress...');

  try {
    if (orderLoadInFlight.current) {
      await orderLoadInFlight.current;
    }

    const csrfToken = await getCsrfToken();
    const order = await command(csrfToken);

    setOrders(previous =>
      previous.map(existing =>
        existing.orderId === order.orderId ? order : existing
      )
    );

    addLog(action + ' successful: Order #' + order.orderId);
    setMessage(action + ' successful');

    await loadRestaurantOrders();
    // Quick stats / queue band follow the order's new state
    await loadQueueSummary();

  } catch (error) {
    showError(action + ' failed', error.message);
  } finally {
    actionInFlight.current = false;
    setPendingAction(null);
  }
}

// Runs one command (cancel, availability, pause/reopen); `afterSuccess` refreshes what the command changed
async function runCommand(action, command, afterSuccess) {
  // Prevent duplicate commands while another request is running
  if (actionInFlight.current) return;

  actionInFlight.current = true;
  setPendingAction(action);
  setMessage(action + ' in progress...');

  try {
    const csrfToken = await getCsrfToken();
    await command(csrfToken);

    addLog(action + ' successful');
    setMessage(action + ' successful');

    if (afterSuccess) await afterSuccess();
  } catch (error) {
    showError(action + ' failed', error.message);
  } finally {
    actionInFlight.current = false;
    setPendingAction(null);
  }
}

// Customer: own orders (UC-ORD-07 follow progress)
async function loadCustomerOrders() {
  setCustomerOrdersLoading(true);
  try {
    const data = await getCustomerOrders();
    setCustomerOrders(data);
    addLog('Your orders loaded (' + data.length + ' orders)');
  } catch (error) {
    showError('Error loading your orders', error.message);
  } finally {
    setCustomerOrdersLoading(false);
  }
}

// Customer: UC-ORD-03 cancel while PLACED
function handleCancelOrder(orderId) {
  return runCommand(
    'Cancel order #' + orderId,
    csrfToken => cancelOrder(orderId, csrfToken),
    loadCustomerOrders
  );
}

// Restaurant (own menu): UC-ORD-05 item availability
function handleToggleAvailability(item) {
  const restaurantId = selectedRestaurantId;
  const soldOut = item.status === 'AVAILABLE';
  return runCommand(
    (soldOut ? 'Mark sold out: ' : 'Mark available: ') + item.name,
    csrfToken => soldOut
      ? markItemSoldOut(restaurantId, item.itemId, csrfToken)
      : markItemAvailable(restaurantId, item.itemId, csrfToken),
    () => loadMenu(restaurantId)
  );
}

// Restaurant (own) and operator (any): UC-ORD-06 pause / reopen
function handleRestaurantStatus(restaurant) {
  const pausing = restaurant.status === 'OPEN';
  return runCommand(
    (pausing ? 'Pause ' : 'Reopen ') + restaurant.name,
    csrfToken => pausing
      ? pauseRestaurant(restaurant.restaurantId, csrfToken)
      : reopenRestaurant(restaurant.restaurantId, csrfToken),
    loadRestaurants
  );
}

function startReject(orderId) {
  setRejectingOrderId(orderId);
  setRejectionReason('');
}

function cancelReject() {
  setRejectingOrderId(null);
  setRejectionReason('');
}

async function handleReject(orderId) {
  const reason = rejectionReason.trim();

  if (!reason) {
    showError('Reject failed', 'A rejection reason is required.');
    return;
  }

  cancelReject();

  await runOrderAction('Reject', csrfToken => rejectOrder(orderId, reason, csrfToken));
}

function openMenuTab() {
  if (selectedRestaurantId === null) {
    addLog('Menu: no restaurant selected');
    // The only pop-up whose OK takes the user somewhere: back to the restaurants to pick one
    setNotice({
      title: 'No restaurant selected',
      message: 'Please select a restaurant first to view its menu.',
      returnTab: 'restaurants'
    });
    return;
  }
  setActiveTab('menu');
}

// Restaurant staff: open the menu of their own restaurant only
function openMyMenu() {
  if (currentUser?.restaurantId == null) {
    showError('Error loading menu', 'No restaurant is assigned to this account.');
    return;
  }
  loadMenu(currentUser.restaurantId);
}

// OK closes the pop-up and keeps the user on the same tab, unless the pop-up says where to go
function closeNotice() {
  const returnTab = notice?.returnTab;
  setNotice(null);
  if (returnTab) setActiveTab(returnTab);
}

// Confirmation pop-up: runs the confirmed command after closing
function confirmNotice() {
  const onConfirm = notice?.onConfirm;
  setNotice(null);
  if (onConfirm) onConfirm();
}

// Reset (operator): ask for confirmation first, then restore the initial state
function handleReset() {
  setNotice({
    title: 'Reset the system?',
    message: 'This removes all orders and deliveries and restores restaurants, menus and couriers to their initial state. Accounts are kept.',
    confirmLabel: 'Reset',
    onConfirm: () => runCommand(
      'Reset',
      csrfToken => resetSystem(csrfToken),
      async () => {
        setMenu([]);
        setSelectedRestaurantId(null);
        setQuantities({});
        setOrders([]);
        setCustomerOrders([]);
        await loadRestaurants();
        await loadQueueSummary();
        addLog('System restored to its initial state');
      }
    )
  });
}

// Only shown on the order whose turn it is (RestaurantOrderView decides), so no pop-up is needed
async function handleStartPreparation(order) {
  await runOrderAction('Start Preparation', csrfToken => startPreparation(order.restaurantId, csrfToken));
}
   

  function switchAuthMode(nextSigningUp) {
    setSigningUp(nextSigningUp);
    setPassword('');
    setMessage('');
  }

  async function handleRegistration(event) {
    event.preventDefault();
    if (registrationPending) return;

    setRegistrationPending(true);
    setMessage('');
    const normalizedEmail = email.trim().toLowerCase();

    try {
      const response = await register({ name: name.trim(), email: normalizedEmail, password });

      if (response.status === 400) {
        setMessage((await response.text()) || 'Check your registration details and try again.');
        return;
      }
      if (!response.ok) {
        setMessage('Could not create your account. Please try again.');
        return;
      }

      setEmail(normalizedEmail);
      setName('');
      switchAuthMode(false);
      setMessage('Account created. Log in with your new password.');
    } catch {
      setMessage('Could not connect to the server. Please try again.');
    } finally {
      setRegistrationPending(false);
    }
  }

  async function handleLogin(event) {
    event.preventDefault();
    setMessage('');

    try {
      const loginAccepted = await login({ email, password });

      if (loginAccepted) {
        const user = await getCurrentUser();
        setOrders([]);
        setCustomerOrders([]);
        setNotice(null);
        setCurrentUser(user);
        setLogs([]);
        setLoggedIn(true);
        setActiveTab(defaultTabFor(user.role));
        addLog('Login successful: ' + email);
        setMessage('Login successful');
      } else {
        addLog('Login failed: ' + email);
        setMessage('Invalid email or password');
      }
    } catch (error) {
      setMessage('Login request failed');
      console.error(error);
    }
  }

  async function handleLogout() {
    try {
      const loggedOut = await logout();

      if (loggedOut) {
        addLog('Logout successful: ' + email);
        setLoggedIn(false);
        setCurrentUser(null);
        setLogs([]);
        setPassword('');
        setRestaurants([]);
        setMenu([]);
        setSelectedRestaurantId(null);
        setQuantities({});
        setOrders([]);
        setCustomerOrders([]);
        setQueueSummary({});
        setNotice(null);
        setActiveTab('restaurants');
        setMessage('Logged out successfully');
      } else {
        showError('Logout failed', 'The server did not end the session. Please try again.');
      }
    } catch (error) {
      showError('Logout failed', 'Could not reach the server. Please try again.');
      console.error(error);
    }
  }

    const filteredRestaurants = restaurants.filter(restaurant => {
    if (selectedCategory === 'All') {
      return true;
    }

    return restaurantCategories[restaurant.restaurantId] === selectedCategory;
  });

  const selectedRestaurant = restaurants.find(
    restaurant => restaurant.restaurantId === selectedRestaurantId
  );

  // R-DASH-01 / Spec Appendix A: each role only sees the commands it is permitted to use
  const role = currentUser?.role;
  const isCustomer = role === 'CUSTOMER';
  const isRestaurant = role === 'RESTAURANT';
  // Customers browse menus; operators see restaurant statuses (pause/reopen any restaurant)
  const canSeeRestaurantList = isCustomer || role === 'OPERATOR';
  // The Orders tab groups the menu and order screens (customer: Menu / My Orders; restaurant: Restaurant Orders / My Menu)
  const inOrdersTab = ['menu', 'myorders', 'orders'].includes(activeTab);

  // Info area "selected restaurant": staff = their own restaurant; customer = the menu they opened
  const infoRestaurant = isRestaurant
    ? restaurants.find(restaurant => restaurant.restaurantId === currentUser?.restaurantId)
    : (isCustomer ? selectedRestaurant : undefined);

  if (loggedIn) {
    return (
      <div className="dashboard-container">
        <div className="dashboard-panel">
          <h1>CARE Eats</h1>
          <h2>Dashboard</h2>

          <InfoArea
            user={currentUser}
            email={email}
            restaurant={infoRestaurant}
            stats={infoRestaurant ? queueSummary[infoRestaurant.restaurantId] : undefined}
            canReset={role === 'OPERATOR'}
            busy={pendingAction !== null}
            onReset={handleReset}
          />

          {/* Spec DASH › Goal: "A horizontal tab bar for the modules (Map, Orders, Delivery, Pricing & History)".
              Each role only sees the tabs it may use (R-DASH-01). */}
          <nav className="dashboard-tabs">
            {canSeeRestaurantList && (
              <button
                type="button"
                className={activeTab === 'restaurants' ? 'active' : ''}
                onClick={() => setActiveTab('restaurants')}
              >
                Map
              </button>
            )}

            {(isCustomer || isRestaurant) && (
              <button
                type="button"
                className={inOrdersTab ? 'active' : ''}
                onClick={() => setActiveTab(isRestaurant ? 'orders' : 'myorders')}
              >
                Orders
              </button>
            )}

            {['COURIER', 'OPERATOR'].includes(role) && (
              <button
                type="button"
                className={activeTab === 'delivery' ? 'active' : ''}
                onClick={() => setActiveTab('delivery')}
              >
                Delivery
              </button>
            )}

            <button
              type="button"
              className={activeTab === 'logs' ? 'active' : ''}
              onClick={() => setActiveTab('logs')}
            >
              Pricing & History
            </button>
          </nav>

          {/* Orders tab: one sub-bar per role */}
          {inOrdersTab && isCustomer && (
            <div className="category-tabs sub-tabs">
              <button
                type="button"
                className={activeTab === 'menu' ? 'active' : ''}
                onClick={openMenuTab}
              >
                {selectedRestaurant && activeTab !== 'menu'
                  ? 'View previous menu: ' + selectedRestaurant.name
                  : 'Menu'}
              </button>

              <button
                type="button"
                className={activeTab === 'myorders' ? 'active' : ''}
                onClick={() => setActiveTab('myorders')}
              >
                My Orders
              </button>
            </div>
          )}

          {inOrdersTab && isRestaurant && (
            <div className="category-tabs sub-tabs">
              <button
                type="button"
                className={activeTab === 'orders' ? 'active' : ''}
                onClick={() => setActiveTab('orders')}
              >
                Restaurant Orders
              </button>

              {/* Restaurant staff only see their own menu (Appendix A: not other restaurants' menus) */}
              <button
                type="button"
                className={activeTab === 'menu' ? 'active' : ''}
                onClick={openMyMenu}
              >
                My Menu
              </button>
            </div>
          )}

          
          {activeTab === 'restaurants' && canSeeRestaurantList && (
            <section>
              <h3>Explore Restaurants</h3>

              <div className="category-tabs">
                {categories.map(category => (
                  <button
                    key={category}
                    type="button"
                    className={selectedCategory === category ? 'active' : ''}
                    onClick={() => setSelectedCategory(category)}
                  >
                    {category}
                  </button>
                ))}
              </div>

              <div className="restaurant-grid">
                {filteredRestaurants.map(restaurant => (
                  <div className="restaurant-card" key={restaurant.restaurantId}>
                    <h4>{restaurant.name}</h4>

                    <p>{restaurantCategories[restaurant.restaurantId] ?? 'Other'}</p>

                    <p>
                      Status: {restaurant.status}
                    </p>

                    {/* R-DASH-03: preparation-queue colour band */}
                    <QueueBand
                      queued={queueSummary[restaurant.restaurantId]?.queued ?? 0}
                      restaurantStatus={restaurant.status}
                    />

                    {/* Only customers browse menus (operators only see the status) */}
                    {isCustomer && (
                      <button
                        type="button"
                        onClick={() => loadMenu(restaurant.restaurantId)}
                      >
                        View Menu
                      </button>
                    )}

                    {/* UC-ORD-06 / R-DASH-01: the operator pauses or reopens any restaurant */}
                    {role === 'OPERATOR' && restaurant.status !== 'CLOSED' && (
                      <button
                        type="button"
                        disabled={pendingAction !== null}
                        onClick={() => handleRestaurantStatus(restaurant)}
                      >
                        {restaurant.status === 'OPEN' ? 'Pause' : 'Reopen'}
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </section>
          )}


          {activeTab === 'menu' && (isCustomer || isRestaurant) && (
            <MenuView
              selectedRestaurant={selectedRestaurant}
              selectedRestaurantId={selectedRestaurantId}
              menu={menu}
              canOrder={isCustomer}
              canManage={isRestaurant}
              busy={pendingAction !== null}
              onToggleAvailability={handleToggleAvailability}
              onRestaurantStatus={handleRestaurantStatus}
              quantities={quantities}
              onQuantityChange={(itemId, value) =>
                setQuantities(previous => ({ ...previous, [itemId]: value }))}
              onPlaceOrder={handlePlaceOrder}
            />
          )}

          {activeTab === 'myorders' && isCustomer && (
            <CustomerOrderView
              orders={customerOrders}
              loading={customerOrdersLoading}
              busy={pendingAction !== null}
              onRefresh={loadCustomerOrders}
              onCancel={handleCancelOrder}
            />
          )}

         {activeTab === 'orders' && currentUser?.role === 'RESTAURANT' && (
            <RestaurantOrderView
              orders={orders}
              restaurantStatus={
                restaurants.find(restaurant => restaurant.restaurantId === currentUser.restaurantId)?.status
              }
              ordersLoading={ordersLoading}
              pendingAction={pendingAction}
              rejectingOrderId={rejectingOrderId}
              rejectionReason={rejectionReason}
              onRejectionReasonChange={setRejectionReason}
              onRefresh={loadRestaurantOrders}
              onAccept={orderId =>
                runOrderAction('Accept', csrfToken => acceptOrder(orderId, csrfToken))}
              onStartReject={startReject}
              onConfirmReject={handleReject}
              onCancelReject={cancelReject}
              onStartPreparation={handleStartPreparation}
              onMarkReady={orderId =>
                runOrderAction('Mark Ready', csrfToken => markOrderReady(orderId, csrfToken))}
            />
          )}
{activeTab === 'delivery'
  && ['COURIER', 'OPERATOR'].includes(currentUser?.role) && (
    <DeliveryConsole
      key={currentUser.userId}
      user={currentUser}
      addLog={addLog}
      getCsrfToken={getCsrfToken}
    />
)}
          {activeTab === 'logs' && currentUser && (
            <PrcConsole key={currentUser.userId} user={currentUser} logs={logs}
              addLog={addLog} getCsrfToken={getCsrfToken} />
          )}

          <p role="status">{message}</p>

          <button type="button" onClick={handleLogout}>
            Log Out
          </button>

          {notice && (
            <div className="notice-backdrop">
              <div className="notice-box" role="alertdialog" aria-modal="true" aria-label={notice.title}>
                <h4>{notice.title}</h4>
                <p>{notice.message}</p>
                {notice.onConfirm ? (
                  <div className="notice-actions">
                    <button type="button" onClick={closeNotice} autoFocus>
                      Cancel
                    </button>
                    <button type="button" onClick={confirmNotice}>
                      {notice.confirmLabel ?? 'Confirm'}
                    </button>
                  </div>
                ) : (
                  <button type="button" onClick={closeNotice} autoFocus>
                    OK
                  </button>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    );
  }

  return (
    <LoginView
      signingUp={signingUp}
      name={name}
      email={email}
      password={password}
      registrationPending={registrationPending}
      message={message}
      onNameChange={setName}
      onEmailChange={setEmail}
      onPasswordChange={setPassword}
      onSubmit={signingUp ? handleRegistration : handleLogin}
      onToggleMode={() => switchAuthMode(!signingUp)}
    />
  );
}

export default App;
