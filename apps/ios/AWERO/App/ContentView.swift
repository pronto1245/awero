import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var wakeFlow: WakeFlowController

    var body: some View {
        Group {
            switch wakeFlow.state {
            case .ringing, .mission, .completed, .emergencyStopped, .storageError:
                WakeScreen(flow: wakeFlow)
            case .idle:
                HomeView()
            }
        }
    }
}
