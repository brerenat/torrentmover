// delegate event for performance, and save attaching a million events to each anchor
document.addEventListener('click', function(event) {
  var target = event.target;
  if (target.tagName.toLowerCase() == 'a') {
      var port = target.getAttribute('href').match(/^:(\d+)(.*)/);
      if (port) {
         target.href = window.location.origin;
         target.port = port[1];
      }
  }
}, false);

if ('serviceWorker' in navigator && 'PushManager' in window) {
  console.log('Service Worker and Push is supported');

  navigator.serviceWorker.register('sw.js')
  .then(function(swReg) {
    console.log('Service Worker is registered', swReg);

    swRegistration = swReg;
    initializeUI();
  })
  .catch(function(error) {
    console.error('Service Worker Error', error);
  });
} else {
  console.warn('Push messaging is not supported');
  pushButton.textContent = 'Push Not Supported';
}

const publicKey = "BOhYqWK66Jh6c52yc44L_x_EufBp0xrPf3D-W_VzbaaNewGMg_sk2ELBc5bIaRW8JGxfGh83FBQ4qp8jIR6BGIo";

function initializeUI() {
	$('.notification-button').click(function() {
		this.disabled = true;
	    if (isSubscribed) {
	      unsubscribeUser();
	    } else {
	      subscribeUser();
	    }
	});
	
  // Set the initial subscription value
  swRegistration.pushManager.getSubscription()
  .then(function(subscription) {
    isSubscribed = !(subscription === null);

    if (isSubscribed) {
      console.log('User IS subscribed.');
    } else {
      console.log('User is NOT subscribed.');
    }

    updateBtn();
  });
}

function updateBtn() {
	$('.notification-button').attr("disabled", false);
}

function subscribeUser() {
  const applicationServerKey = urlB64ToUint8Array(publicKey);
  swRegistration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: applicationServerKey
  })
  .then(function(subscription) {
    console.log('User is subscribed.');

    updateSubscriptionOnServer(subscription);

    isSubscribed = true;

    updateBtn();
  })
  .catch(function(err) {
    console.log('Failed to subscribe the user: ', err);
    updateBtn();
  });
}

function unsubscribeUser() {
  swRegistration.pushManager.getSubscription()
  .then(function(subscription) {
    if (subscription) {
      return subscription.unsubscribe();
    }
  })
  .catch(function(error) {
    console.log('Error unsubscribing', error);
  })
  .then(function() {
    updateSubscriptionOnServer(null);

    console.log('User is unsubscribed.');
    isSubscribed = false;

    updateBtn();
  });
}

function updateSubscriptionOnServer(subscription) {
  // TODO: Send subscription to application server

  const subscriptionJson = $('.js-subscription-json');
  const subscriptionDetails = $('.js-subscription-details');

  console.log(subscription);
  
  if (subscription) {
    subscriptionJson.html(JSON.stringify(subscription));
    subscriptionDetails.show();
    sendToServer(JSON.stringify(subscription));
  } else {
    subscriptionDetails.hide();
  }
}

function sendToServer(subIn) {
	var sub = JSON.parse(subIn);
	console.log("Sending to Server");
	console.log(sub);
	var toSend = {
		"endpoint" : sub.endpoint,
		"auth" : sub.keys.auth,
		"p256dh" : sub.keys.p256dh
	}
	$.ajax({
		url : "/rest/saveSub",
		data : toSend,
		success : function(res) {
			alert(res.result);
		},
		error: function(e1, e2, e3) {
			alert(e1 + "\r\n" + e2 + "\r\n" + e3);
		}
	})
}

function urlB64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - base64String.length % 4) % 4);
  const base64 = (base64String + padding)
    .replace(/\-/g, '+')
    .replace(/_/g, '/');

  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);

  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}