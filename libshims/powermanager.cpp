/*
 * Android 5.1's native IPowerManager for Huawei's libmm-abl.so (mm-pp-daemon).
 * The blob only imports asInterface() and then calls through the returned
 * object's vtable, so the class is declared in 5.1's layout. Every slot returns
 * NO_ERROR without contacting PowerManagerService (its interface has changed
 * since 5.1): the daemon only loses holding its own wakelock, and the trailing
 * padding slots make any unexpected call land in a harmless "OK" as well.
 */
#include <binder/Binder.h>
#include <binder/IInterface.h>
#include <utils/String16.h>

namespace android {

class IPowerManager : public IInterface {
  public:
    static sp<IPowerManager> asInterface(const sp<IBinder>& obj);
    virtual const String16& getInterfaceDescriptor() const = 0;

    virtual status_t acquireWakeLock(int flags, const sp<IBinder>& lock, const String16& tag,
            const String16& packageName, bool isOneWay = false) = 0;
    virtual status_t acquireWakeLockWithUid(int flags, const sp<IBinder>& lock,
            const String16& tag, const String16& packageName, int uid,
            bool isOneWay = false) = 0;
    virtual status_t releaseWakeLock(const sp<IBinder>& lock, int flags,
            bool isOneWay = false) = 0;
    virtual status_t updateWakeLockUids(const sp<IBinder>& lock, int len, const int* uids,
            bool isOneWay = false) = 0;
    virtual status_t powerHint(int hintId, int data) = 0;
    virtual status_t reserved0() = 0;
    virtual status_t reserved1() = 0;
    virtual status_t reserved2() = 0;
    virtual status_t reserved3() = 0;
};

namespace {

class ShimPowerManager : public IPowerManager {
  public:
    const String16& getInterfaceDescriptor() const override { return mDescriptor; }
    status_t acquireWakeLock(int, const sp<IBinder>&, const String16&, const String16&,
            bool) override { return NO_ERROR; }
    status_t acquireWakeLockWithUid(int, const sp<IBinder>&, const String16&,
            const String16&, int, bool) override { return NO_ERROR; }
    status_t releaseWakeLock(const sp<IBinder>&, int, bool) override { return NO_ERROR; }
    status_t updateWakeLockUids(const sp<IBinder>&, int, const int*, bool) override {
        return NO_ERROR;
    }
    status_t powerHint(int, int) override { return NO_ERROR; }
    status_t reserved0() override { return NO_ERROR; }
    status_t reserved1() override { return NO_ERROR; }
    status_t reserved2() override { return NO_ERROR; }
    status_t reserved3() override { return NO_ERROR; }

  protected:
    // A real (local) binder, in case the blob links to death or compares binders.
    IBinder* onAsBinder() override { return mBinder.get(); }

  private:
    const String16 mDescriptor{"android.os.IPowerManager"};
    const sp<BBinder> mBinder = sp<BBinder>::make();
};

}  // namespace

sp<IPowerManager> IPowerManager::asInterface(const sp<IBinder>& /*obj*/) {
    return sp<ShimPowerManager>::make();
}

}  // namespace android
